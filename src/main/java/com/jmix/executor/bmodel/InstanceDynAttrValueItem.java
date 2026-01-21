package com.jmix.executor.bmodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class InstanceDynAttrValueItem extends Extensible {
    /**
     * 实例ID
     */
    private int instId;

    /**
     * 实例属性映射
     */
    private Map<String, String> instAttrs = new HashMap<>();

    @JsonIgnore
    public String getInstAttr(String key) {
        return instAttrs.get(key);
    }

    /**
     * 将对象转换为JSON字符串
     *
     * @param iValue 实例属性值对象
     * @return JSON字符串
     */
    public static String toJsonString(InstanceDynAttrValueItem iValue) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(iValue);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize InstanceDynAttrValueItem to JSON", e);
        }
    }

    /**
     * 从JSON字符串转换为对象
     *
     * @param strValue JSON字符串
     * @return 实例属性值对象
     */
    public static InstanceDynAttrValueItem fromJsonString(String strValue) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(strValue, InstanceDynAttrValueItem.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to InstanceDynAttrValueItem", e);
        }
    }
}
