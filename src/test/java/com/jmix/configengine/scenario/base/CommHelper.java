package com.jmix.configengine.scenario.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;

/**
 * 通用工具类
 * 提供测试场景中常用的工具方法
 */
public class CommHelper {
    
    private static final Logger log = LoggerFactory.getLogger(CommHelper.class);
    
    /**
     * 创建临时路径
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
     * @param clazz 类
     * @return 资源路径
     */
    public static String getResourcePath(Class<?> clazz) {
        String packagePath = clazz.getPackage().getName().replace('.', '/');
        return "src/test/java/" + packagePath;
    }
    
    /**
     * 创建目录
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