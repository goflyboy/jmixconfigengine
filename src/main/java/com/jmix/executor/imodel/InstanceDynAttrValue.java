package com.jmix.executor.imodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 多实例类型属性值
 * 表示部件的多个实例，每个实例有不同的属性值
 *
 * @since 2025-12-27
 */
@Data
public class InstanceDynAttrValue {
    /**
     * 实例ID
     */
    private int instId;

    /**
     * 实例属性映射
     */
    private Map<String, String> instAttr = new HashMap<>();

    /**
     * 将对象转换为JSON字符串
     *
     * @param iValue 实例属性值对象
     * @return JSON字符串
     */
    public static String toJsonString(InstanceDynAttrValue iValue) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(iValue);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize InstanceDynAttrValue to JSON", e);
        }
    }

    /**
     * 从JSON字符串转换为对象
     *
     * @param strValue JSON字符串
     * @return 实例属性值对象
     */
    public static InstanceDynAttrValue fromJsonString(String strValue) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(strValue, InstanceDynAttrValue.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to InstanceDynAttrValue", e);
        }
    }
}
