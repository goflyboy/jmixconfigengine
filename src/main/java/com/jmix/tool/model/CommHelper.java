package com.jmix.tool.model;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * 通用工具类
 * 提供测试场景中常用的工具方法
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
        String tempPath = getResourcePath(constraintAlgClazz) + "/tempResource";
        createDirectory(tempPath);
        return tempPath;
    }

    /**
     * 获取资源路径
     * 
     * @param clazz 类
     * @return 资源路径
     */
    public static String getResourcePath(Class<?> clazz) {
        String currentDir = clazz.getResource(".").getPath();
        if (currentDir.startsWith("/")) {
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

        String packagePath = clazz.getPackage().getName().replace('.', '/');
        if (!currentDir.endsWith("/")) {
            currentDir = currentDir + "/";
        }
        return currentDir + "src/test/java/" + packagePath;
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
                log.info("创建目录: {}", path);
            } else {
                log.warn("创建目录失败: {}", path);
            }
        }
    }
}