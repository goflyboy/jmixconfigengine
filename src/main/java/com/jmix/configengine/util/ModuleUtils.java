package com.jmix.configengine.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jmix.configengine.model.Module;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Module工具类
 * 提供Module对象的JSON序列化和反序列化功能
 */
@Slf4j
public class ModuleUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        // 配置ObjectMapper
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // 美化输出
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS); // 允许空Bean序列化
        
        // 配置类型信息：使用PROPERTY形式携带@type
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
    }
    
    /**
     * 将Module对象序列化为JSON文件
     * 
     * @param module Module对象
     * @param jsonFileName JSON文件名（包含路径）
     * @throws IOException 如果文件操作失败
     */
    public static void toJsonFile(Module module, String jsonFileName) throws IOException {
        log.info("Serializing Module to JSON file: {}", jsonFileName);
        
        // 确保目录存在
        Path path = Paths.get(jsonFileName);
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        
        // 序列化为JSON文件
        objectMapper.writeValue(new File(jsonFileName), module);
        
        log.info("Successfully serialized Module to JSON file: {}", jsonFileName);
    }
    
    /**
     * 从JSON文件反序列化为Module对象
     * 
     * @param jsonFileName JSON文件名（包含路径）
     * @return Module对象
     * @throws IOException 如果文件操作失败
     */
    public static Module fromJsonFile(String jsonFileName) throws IOException {
        log.info("Deserializing Module from JSON file: {}", jsonFileName);
        
        // 检查文件是否存在
        File file = new File(jsonFileName);
        if (!file.exists()) {
            throw new IOException("JSON file not found: " + jsonFileName);
        }
        
        // 反序列化
        Module module = objectMapper.readValue(file, Module.class);
        
        // 初始化模块
        module.init();
        
        log.info("Successfully deserialized Module from JSON file: {}", jsonFileName);
        return module;
    }
    
    /**
     * 将Module对象序列化为JSON字符串
     * 
     * @param module Module对象
     * @return JSON字符串
     * @throws IOException 如果序列化失败
     */
    public static String toJsonString(Module module) throws IOException {
        log.debug("Serializing Module to JSON string");
        return objectMapper.writeValueAsString(module);
    }
    
    /**
     * 从JSON字符串反序列化为Module对象
     * 
     * @param jsonString JSON字符串
     * @return Module对象
     * @throws IOException 如果反序列化失败
     */
    public static Module fromJsonString(String jsonString) throws IOException {
        log.debug("Deserializing Module from JSON string");
        Module module = objectMapper.readValue(jsonString, Module.class);
        module.init();
        return module;
    }
    
    /**
     * 验证JSON文件格式是否正确
     * 
     * @param jsonFileName JSON文件名
     * @return 如果格式正确返回true，否则返回false
     */
    public static boolean validateJsonFile(String jsonFileName) {
        try {
            fromJsonFile(jsonFileName);
            return true;
        } catch (Exception e) {
            log.warn("JSON file validation failed: {}", e.getMessage());
            return false;
        }
    }
} 