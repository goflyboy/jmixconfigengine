package com.jmix.executor.impl;

import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.imodel.ModuleAlgArtifact;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.omodel.AlgLoaderException;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private ConstraintConfig config;

    private String constraintRuleClassName;

    private final Map<String, Class<?>> classMap = new HashMap<>();

    private ModuleAlgArtifact algArtifact;

    private URLClassLoader jarClassLoader;

    /**
     * 构造函数
     * 
     * @param isAttachedDebug 是否附加调试信息
     * @param rootFilePath    根文件路径
     */
    public ModuleAlgClassLoader(ConstraintConfig config, ModuleAlgArtifact algArtifact) {
        this.config = config;
        this.algArtifact = algArtifact;
    }

    /**
     * 初始化模块算法类加载器
     */
    public void init() {
        // 构建完整的类名，支持内部类场景
        StringBuilder classNameBuilder = new StringBuilder();
        // 添加包名和类名
        classNameBuilder.append(algArtifact.getPackageName()).append(".");
        // 如果有父类名称，先添加父类
        if (algArtifact.getParentClassName() != null
                && !algArtifact.getParentClassName().isEmpty()) {
            classNameBuilder.append(algArtifact.getParentClassName()).append("$");
        }
        classNameBuilder.append(algArtifact.getModuleCode()).append("Constraint");
        this.constraintRuleClassName = classNameBuilder.toString();

        // 如果isAttachedDebug=false，从rootFilePath读取class信息
        if (!config.isAttachedDebug()) {
            initJarClassLoader();
        }
    }

    /**
     * 初始化jar包类加载器
     * 
     * @param moduleCode 模块代码
     */
    private void initJarClassLoader() {
        try {
            // 构建模块文件目录路径
            String moduleFileDir = config.getRootFilePath() + File.separator + "cp-" + algArtifact.getModuleCode()
                    + "-" + algArtifact.getId();
            String classJarFile = moduleFileDir + File.separator + "cp-" + algArtifact.getModuleCode() + "-"
                    + algArtifact.getId()
                    + ".jar";

            log.info("Loading classes from jar: {}", classJarFile);

            // 检查jar文件是否存在
            Path jarPath = Paths.get(classJarFile);
            if (!Files.exists(jarPath)) {
                throw new AlgLoaderException("Jar file not found: " + classJarFile);
            }

            // 创建URLClassLoader加载jar包
            URL jarUrl = jarPath.toUri().toURL();
            jarClassLoader = new URLClassLoader(new URL[] { jarUrl }, this.getClass().getClassLoader());

            // 预加载所有class文件
            preloadClassesFromJar(classJarFile);

            log.info("Jar class loader initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize jar class loader", e);
            throw new AlgLoaderException("Failed to initialize jar class loader: " + e.getMessage(), e);
        }
    }

    /**
     * 从jar包预加载所有class文件
     * 
     * @param jarFilePath jar文件路径
     */
    private void preloadClassesFromJar(String jarFilePath) {
        try (JarFile jarFile = new JarFile(jarFilePath)) {
            jarFile.stream()
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .forEach(entry -> {
                        try {
                            String className = entry.getName().replace("/", ".").replace(".class", "");
                            className = algArtifact.getPackageName() + "." + className;
                            log.info("Processing jar entry: {} -> className: {}", entry.getName(), className);
                            loadClassFromJar(className, jarFile, entry);
                        } catch (Throwable e) {
                            log.error("Failed to preload class: {}", entry.getName(), e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to read jar file: {}", jarFilePath, e);
            throw new AlgLoaderException("Failed to read jar file: " + jarFilePath, e);
        }
    }

    /**
     * 从jar包加载单个class文件
     * 
     * @param className 类名
     * @param jarFile   jar文件
     * @param entry     jar条目
     */
    private void loadClassFromJar(String className, JarFile jarFile, JarEntry entry) {
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            byte[] classBytes = readAllBytes(inputStream);
            Class<?> clazz = super.defineClass(className, classBytes, 0, classBytes.length);
            classMap.put(className, clazz);
            log.info("Loaded class from jar: {}", className);
        } catch (Throwable e) {
            log.error("Failed to load class from jar: {}", className, e);
        }
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
     * @throws Exception 创建过程中的异常
     */
    public ConstraintAlgImpl newConstraintAlg(String moduleCode) throws Exception {
        String className = this.constraintRuleClassName;
        Class<?> clazz = classMap.get(className);
        if (clazz == null) {
            clazz = loadClass(className);
            classMap.put(className, clazz);
        }
        Object instance = clazz.getDeclaredConstructor().newInstance();
        if (!(instance instanceof ConstraintAlgImpl)) {
            throw new AlgLoaderException("Loaded class is not an instance of ConstraintAlgImpl: " + className);
        }
        return (ConstraintAlgImpl) instance;
    }

    /**
     * 加载类
     * 
     * @param name 类名
     * @return 加载的类
     */
    @Override
    public Class<?> loadClass(String name) {
        try {
            if (config.isAttachedDebug()) {
                // 调试模式：使用现有逻辑
                return super.loadClass(name);
            } else {
                // 打包模式：从预加载的class信息加载
                Class<?> clazz = classMap.get(name);
                if (clazz != null) {
                    return clazz;
                }

                // 如果classMap中没有，尝试使用jarClassLoader加载
                if (jarClassLoader != null) {
                    return jarClassLoader.loadClass(name);
                }

                throw new ClassNotFoundException("Class not found: " + name);
            }
        } catch (ClassNotFoundException e) {
            throw new AlgLoaderException("Class not found: " + name, e);
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (jarClassLoader != null) {
            try {
                jarClassLoader.close();
                log.debug("Jar class loader closed");
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
}