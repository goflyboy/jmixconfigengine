package com.jmix.executor.impl;

import com.jmix.executor.bmodel.ModuleAlgArtifact;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.southinf.AlgorithmApiVersion;

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
 * 模块算法类加载器
 * 负责动态加载和实例化约束算法类
 * 
 * @since 2025-09-22
 */
@Slf4j
public class ModuleAlgClassLoader extends ClassLoader {
    public static final String LATEST_SOUTH_API_VERSION = "1.0";

    /**
     * 约束执行配置，决定调试模式与根目录等加载行为
     */
    private ConstraintConfig config;

    /**
     * 约束规则类的完整限定名（可能包含内部类分隔符 $）
     */
    private String constraintRuleClassName;

    /**
     * 已加载的类缓存映射（类名 -> Class 对象）
     */
    private final Map<String, Class<?>> classMap = new HashMap<>();

    /**
     * 模块算法制品描述（包含包名、模块码、父类名等）
     */
    private ModuleAlgArtifact algArtifact;

    /**
     * 用于加载 jar 中类的 URLClassLoader 实例
     */
    private URLClassLoader jarClassLoader;

    public ModuleAlgClassLoader(ConstraintConfig config, ModuleAlgArtifact algArtifact) {
        this.config = config;
        this.algArtifact = algArtifact;
    }

    /**
     * 初始化模块算法类加载器
     */
    public void init() {
        validateArtifactApiVersion();
        // 构建完整的类名，支持内部类场景
        this.constraintRuleClassName = toFullConstraintClassName(algArtifact);

        // 如果isAttachedDebug=false，从rootFilePath读取class信息
        if (!config.isAttachedDebug()) {
            loadClassFromJar();
        } else {
            loadClassFromLocalProject();
        }
    }

    /**
     * 从本地调试项目中加载class信息
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
     * 从jar包中加载class信息
     */
    private void loadClassFromJar() throws AlgLoaderException {

        // 构建模块文件目录路径和jar文件路径
        String classJarFile = algArtifact.getRuntimeJarPath(config.getRootFilePath());

        log.info("Loading classes from jar: {}", classJarFile);

        // 检查jar文件是否存在
        Path jarPath = Paths.get(classJarFile);
        if (!Files.exists(jarPath)) {
            log.error("Jar file not found: {}", classJarFile);
            throw new AlgLoaderException("Jar file not found: " + classJarFile);
        }

        // 创建URLClassLoader加载jar包
        URL jarUrl = null;
        try {
            jarUrl = jarPath.toUri().toURL();
        } catch (MalformedURLException e) {
            log.error("Failed to initialize jar class loader", e);
            throw new AlgLoaderException("Failed to initialize jar class loader: " + e.getMessage(), e);
        }
        jarClassLoader = new URLClassLoader(new URL[] { jarUrl }, this.getClass().getClassLoader());

        // 预加载所有class文件
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
     * 从jar包预加载所有class文件
     * 
     * @param jarFilePath jar文件路径
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
     * 读取InputStream的所有字节
     * 
     * @param inputStream 输入流
     * @return 字节数组
     * @throws IOException IO异常
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
     * 创建模块算法类实例
     * 
     * @param moduleCode 模块代码
     * @return 约束算法实现实例
     * @throws AlgLoaderException 创建过程中的异常
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
        if (!(instance instanceof ModuleAlgImpl)) {
            log.error("Loaded class is not an instance of ModuleAlgImpl: {}", className);
            throw new AlgLoaderException("Loaded class is not an instance of ModuleAlgImpl: " + className);
        }
        return (ModuleAlgImpl) instance;
    }

    /**
     * 清理资源
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
     * 创建模块算法类加载器实例, 安全访问
     * 
     * @param config      配置
     * @param algArtifact 算法制品
     * @return 模块算法类加载器实例
     */
    public static ModuleAlgClassLoader newInstance(ConstraintConfig config, ModuleAlgArtifact algArtifact) {
        return AccessController.doPrivileged(
                (PrivilegedAction<ModuleAlgClassLoader>) () -> new ModuleAlgClassLoader(config, algArtifact));
    }
}
