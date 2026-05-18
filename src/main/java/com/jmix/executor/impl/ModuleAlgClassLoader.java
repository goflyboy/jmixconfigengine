package com.jmix.executor.impl;

import com.jmix.executor.bmodel.ModuleAlgArtifact;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.impl.southbridge.SouthboundModuleAlgAdapter;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.tool.bbuilder.anno.AlgorithmApiVersion;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 妯″潡绠楁硶绫诲姞杞藉櫒
 * 璐熻矗鍔ㄦ€佸姞杞藉拰瀹炰緥鍖栫害鏉熺畻娉曠被
 * 
 * @since 2025-09-22
 */
@Slf4j
public class ModuleAlgClassLoader extends ClassLoader {
    public static final String LATEST_SOUTH_API_VERSION = "1.0";

    /**
     * 绾︽潫鎵ц閰嶇疆锛屽喅瀹氳皟璇曟ā寮忎笌鏍圭洰褰曠瓑鍔犺浇琛屼负
     */
    private ConstraintConfig config;

    /**
     * 绾︽潫瑙勫垯绫荤殑瀹屾暣闄愬畾鍚嶏紙鍙兘鍖呭惈鍐呴儴绫诲垎闅旂 $锛?
     */
    private String constraintRuleClassName;

    /**
     * 宸插姞杞界殑绫荤紦瀛樻槧灏勶紙绫诲悕 -> Class 瀵硅薄锛?
     */
    private final Map<String, Class<?>> classMap = new HashMap<>();

    /**
     * 妯″潡绠楁硶鍒跺搧鎻忚堪锛堝寘鍚寘鍚嶃€佹ā鍧楃爜銆佺埗绫诲悕绛夛級
     */
    private ModuleAlgArtifact algArtifact;

    /**
     * 鐢ㄤ簬鍔犺浇 jar 涓被鐨?URLClassLoader 瀹炰緥
     */
    private URLClassLoader jarClassLoader;

    public ModuleAlgClassLoader(ConstraintConfig config, ModuleAlgArtifact algArtifact) {
        this.config = config;
        this.algArtifact = algArtifact;
    }

    /**
     * 鍒濆鍖栨ā鍧楃畻娉曠被鍔犺浇鍣?
     */
    public void init() {
        validateArtifactApiVersion();
        // 鏋勫缓瀹屾暣鐨勭被鍚嶏紝鏀寔鍐呴儴绫诲満鏅?
        this.constraintRuleClassName = toFullConstraintClassName(algArtifact);

        // 濡傛灉isAttachedDebug=false锛屼粠rootFilePath璇诲彇class淇℃伅
        if (!config.isAttachedDebug()) {
            loadClassFromJar();
        } else {
            loadClassFromLocalProject();
        }
    }

    /**
     * 浠庢湰鍦拌皟璇曢」鐩腑鍔犺浇class淇℃伅
     */
    private void loadClassFromLocalProject() {
        Class<?> clazz = null;
        try {
            clazz = super.loadClass(this.constraintRuleClassName);
            fillArtifactVersionFromAnnotation(clazz);
            log.info("Loaded class from local project: {}", constraintRuleClassName);
        } catch (ClassNotFoundException e) {
            log.error("Class not found: {}", constraintRuleClassName, e);
            throw new AlgLoaderException("Class not found: " + constraintRuleClassName, e);
        }
        if (clazz != null) {
            validateResolvedApiVersion();
            classMap.put(constraintRuleClassName, clazz);
        } else {
            log.error("Class not found: clazz=null {}", constraintRuleClassName);
            throw new AlgLoaderException("Class not found: clazz=null " + constraintRuleClassName);
        }
    }

    /**
     * 浠巎ar鍖呬腑鍔犺浇class淇℃伅
     */
    private void loadClassFromJar() throws AlgLoaderException {

        // 鏋勫缓妯″潡鏂囦欢鐩綍璺緞鍜宩ar鏂囦欢璺緞
        String classJarFile = algArtifact.getRuntimeJarPath(config.getRootFilePath());

        log.info("Loading classes from jar: {}", classJarFile);

        // 妫€鏌ar鏂囦欢鏄惁瀛樺湪
        Path jarPath = Paths.get(classJarFile);
        if (!Files.exists(jarPath)) {
            log.error("Jar file not found: {}", classJarFile);
            throw new AlgLoaderException("Jar file not found: " + classJarFile);
        }

        // 鍒涘缓URLClassLoader鍔犺浇jar鍖?
        URL jarUrl = null;
        try {
            jarUrl = jarPath.toUri().toURL();
        } catch (MalformedURLException e) {
            log.error("Failed to initialize jar class loader", e);
            throw new AlgLoaderException("Failed to initialize jar class loader: " + e.getMessage(), e);
        }
        jarClassLoader = new URLClassLoader(new URL[] { jarUrl }, this.getClass().getClassLoader());

        // 棰勫姞杞芥墍鏈塩lass鏂囦欢
        preloadClassesFromJar(classJarFile);
        log.info("Jar class loader initialized successfully");

    }

    private String toFullConstraintClassName(ModuleAlgArtifact algArtifact) {
        StringBuilder classNameBuilder = new StringBuilder();
        classNameBuilder.append(algArtifact.getPackageName()).append(".");
        classNameBuilder.append(toSimpleConstraintClassName(algArtifact));
        return classNameBuilder.toString();
    }

    private String toSimpleConstraintClassName(ModuleAlgArtifact algArtifact) {
        StringBuilder classNameBuilder = new StringBuilder();
        if (algArtifact.getParentClassName() != null
                && !algArtifact.getParentClassName().isEmpty()) {
            classNameBuilder.append(algArtifact.getParentClassName()).append("$");
        }
        classNameBuilder.append(algArtifact.getModuleCode()).append("Constraint");
        return classNameBuilder.toString();
    }

    private void validateArtifactApiVersion() {
        String version = trimToNull(algArtifact.getSouthApiVersion());
        if (version == null && !config.isAttachedDebug()) {
            throw new AlgLoaderException(
                    "southApiVersion is required for algorithm artifact "
                            + algArtifact.getModuleCode()
                            + ". Please regenerate the algorithm with southbound API "
                            + LATEST_SOUTH_API_VERSION + ".");
        }
        if (version != null) {
            validateSupportedApiVersion(version);
        }
    }

    private void fillArtifactVersionFromAnnotation(Class<?> clazz) {
        if (trimToNull(algArtifact.getSouthApiVersion()) != null) {
            return;
        }
        AlgorithmApiVersion apiVersion = clazz.getAnnotation(AlgorithmApiVersion.class);
        if (apiVersion == null) {
            return;
        }
        algArtifact.setSouthApiVersion(apiVersion.southApiVersion());
        algArtifact.setAlgorithmVersion(apiVersion.algorithmVersion());
        log.info("Filled south API version from annotation: {}", apiVersion.southApiVersion());
    }

    private void validateResolvedApiVersion() {
        String version = trimToNull(algArtifact.getSouthApiVersion());
        if (version == null) {
            if (config.isAttachedDebug()) {
                log.warn("southApiVersion is missing for local debug algorithm: {}", algArtifact.getModuleCode());
                return;
            }
            throw new AlgLoaderException(
                    "southApiVersion is required for algorithm artifact "
                            + algArtifact.getModuleCode()
                            + ". Please regenerate the algorithm with southbound API "
                            + LATEST_SOUTH_API_VERSION + ".");
        }
        validateSupportedApiVersion(version);
    }

    private void validateSupportedApiVersion(String version) {
        if (LATEST_SOUTH_API_VERSION.equals(version)) {
            return;
        }
        throw new AlgLoaderException(
                "Unsupported southApiVersion " + version
                        + " for module " + algArtifact.getModuleCode()
                        + ". Engine supports " + LATEST_SOUTH_API_VERSION + ".");
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 浠巎ar鍖呴鍔犺浇鎵€鏈塩lass鏂囦欢
     * 
     * @param jarFilePath jar鏂囦欢璺緞
     */
    private void preloadClassesFromJar(String jarFilePath) {
        String simpleConstraintRuleClassName = toSimpleConstraintClassName(algArtifact);
        JarEntry constraintJarEntry = null;
        try (JarFile jarFile = new JarFile(jarFilePath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryClassName = entry.getName().replace("/", ".").replace(".class", "");
                if (entryClassName.equals(simpleConstraintRuleClassName)
                        || entryClassName.equals(this.constraintRuleClassName)) {
                    constraintJarEntry = entry;
                } else {
                    continue;
                }
            }
            loadClassFromJar(this.constraintRuleClassName, jarFile, constraintJarEntry);
        } catch (ClassFormatError | IOException e) {
            log.error("Failed to read jar file: {}", jarFilePath, e);
            throw new AlgLoaderException("Failed to read jar file: " + jarFilePath, e);
        }
    }

    private Class<?> loadClassFromJar(String className, JarFile jarFile, JarEntry entry)
            throws ClassFormatError, IOException {
        if (classMap.containsKey(className)) {
            log.warn(className + "has loaed!, skipped");
            return classMap.get(className);
        }
        if (entry == null) {
            throw new AlgLoaderException("Class entry not found in jar: " + className);
        }
        InputStream inputStream = jarFile.getInputStream(entry);
        byte[] classBytes = readAllBytes(inputStream);
        Class<?> clazz = super.defineClass(className, classBytes, 0, classBytes.length);
        if (className.equals(this.constraintRuleClassName)) {
            fillArtifactVersionFromAnnotation(clazz);
            validateResolvedApiVersion();
        }
        classMap.put(className, clazz);
        log.info("Loaded class from jar: {}", className);
        return clazz;
    }

    /**
     * 璇诲彇InputStream鐨勬墍鏈夊瓧鑺?
     * 
     * @param inputStream 杈撳叆娴?
     * @return 瀛楄妭鏁扮粍
     * @throws IOException IO寮傚父
     */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }

    /**
     * 鍒涘缓妯″潡绠楁硶绫诲疄渚?
     * 
     * @param moduleCode 妯″潡浠ｇ爜
     * @return 绾︽潫绠楁硶瀹炵幇瀹炰緥
     * @throws AlgLoaderException 鍒涘缓杩囩▼涓殑寮傚父
     */
    public ModuleAlgImpl newConstraintAlg(String moduleCode) {
        String className = this.constraintRuleClassName;
        Class<?> clazz = classMap.get(className);
        if (clazz == null) {
            log.error("Class not found: class=null {}", className);
            throw new AlgLoaderException("Class not found:class=null " + className);
        }
        Object instance = null;
        try {
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            log.error("Failed to create instance of ModuleAlgImpl: {}", className, e);
            throw new AlgLoaderException("Failed to create instance of ModuleAlgImpl: " + className, e);
        }
        if (instance instanceof ModuleAlgBase southboundAlgorithm) {
            return new SouthboundModuleAlgAdapter(southboundAlgorithm);
        }
        if (!(instance instanceof ModuleAlgImpl)) {
            log.error("Loaded class is not an instance of ModuleAlgImpl: {}", className);
            throw new AlgLoaderException("Loaded class is not an instance of ModuleAlgImpl: " + className);
        }
        return (ModuleAlgImpl) instance;
    }

    /**
     * 娓呯悊璧勬簮
     */
    public void cleanup() {
        if (jarClassLoader != null) {
            try {
                jarClassLoader.close();
                log.info("Jar class loader closed");
            } catch (IOException e) {
                log.warn("Failed to close jar class loader", e);
            }
        }
    }

    @Override
    public String toString() {
        return "ModuleAlgClassLoader{"
                + "isAttachedDebug="
                + config.isAttachedDebug()
                + ", rootFilePath='"
                + config.getRootFilePath()
                + '\''
                + ", constraintRuleClassName='"
                + constraintRuleClassName
                + '\''
                + ", classMap="
                + classMap
                + ", algArtifact="
                + algArtifact
                + '}';
    }

    /**
     * 鍒涘缓妯″潡绠楁硶绫诲姞杞藉櫒瀹炰緥, 瀹夊叏璁块棶
     * 
     * @param config      閰嶇疆
     * @param algArtifact 绠楁硶鍒跺搧
     * @return 妯″潡绠楁硶绫诲姞杞藉櫒瀹炰緥
     */
    public static ModuleAlgClassLoader newInstance(ConstraintConfig config, ModuleAlgArtifact algArtifact) {
        return AccessController.doPrivileged(
                (PrivilegedAction<ModuleAlgClassLoader>) () -> new ModuleAlgClassLoader(config, algArtifact));
    }
}
