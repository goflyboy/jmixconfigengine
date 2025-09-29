package com.jmix.executor.impl.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * 通用工具类
 * 提供测试场景中常用的工具方法
 * 
 * @since 2025-09-22
 */
@Slf4j
public class CommHelper {
    /**
     * 创建临时路径
     * 
     * @param constraintAlgClazz 约束算法类
     * @return 临时资源路径
     */
    public static String createTempPath(Class<?> constraintAlgClazz) {
        // 生成临时资源路径
        String tempPath = getJavaFilePath(constraintAlgClazz) + File.separator + "tempResource";
        createDirectory(tempPath);
        return tempPath;
    }

    /**
     * 获取资源路径
     * 
     * @param clazz 类
     * @return 资源路径
     */
    public static String getJavaFilePath(Class<?> clazz) {
        String currentDir = clazz.getResource(".").getPath();
        if (currentDir.startsWith(File.separator)) {
            currentDir = currentDir.substring(1);
        }

        // 查找target目录的位置，如果找不到则使用当前目录
        int targetIndex = currentDir.indexOf("\\target");
        if (targetIndex == -1) {
            targetIndex = currentDir.indexOf("/target");
        }

        if (targetIndex != -1) {
            currentDir = currentDir.substring(0, targetIndex);
        }
        if (currentDir.startsWith("/")) {
            currentDir = currentDir.substring(1, currentDir.length());
        }
        String packagePath = clazz.getPackage().getName().replace('.', File.separatorChar);
        if (!currentDir.endsWith(File.separator)) {
            currentDir = currentDir + File.separator;
        }
        return currentDir + "src" + File.separator + "test" + File.separator + "java" + File.separator + packagePath;
    }

    /**
     * 创建目录
     * 
     * @param path 目录路径
     */
    public static void createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.info("Created directory: {}", path);
            } else {
                log.warn("Failed to create directory: {}", path);
            }
        }
    }

    /**
     * 获取当前测试资源路径
     * 
     * @return 测试资源路径
     */
    public static String getTestResourcePath() {
        return System.getProperty("user.dir") + File.separator + "src" + File.separator + "test" + File.separator
                + "resources";
    }

    /**
     * 获取当前代码的资源路径
     * 
     * @return 代码资源路径
     */
    public static String getCodeResourcePath() {
        return System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator
                + "resources";
    }
}