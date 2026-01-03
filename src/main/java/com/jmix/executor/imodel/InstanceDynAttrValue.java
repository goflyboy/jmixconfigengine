package com.jmix.executor.imodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 多实例类型属性值
 * 管理多个实例属性值的容器类
 *
 * @since 2025-12-27
 */
@Data
public class InstanceDynAttrValue {
    /**
     * 实例属性值列表
     */
    private List<InstanceDynAttrValueItem> instsValues = new ArrayList<>();

    /**
     * 根据条件查询实例属性值
     * 例如：whereCondition="Speed like %5400%"，表示查询 attrs["speed"] like %5400%的InstanceDynAttrValueItem
     *
     * @param whereCondition 查询条件表达式
     * @return 匹配的实例属性值列表
     */
    public List<InstanceDynAttrValueItem> query(String whereCondition) {
        if (whereCondition == null || whereCondition.trim().isEmpty()) {
            return new ArrayList<>(instsValues);
        }

        // 解析查询条件，目前支持简单的 like 查询
        // 格式：fieldName like %value%
        String[] parts = whereCondition.split("\\s+like\\s+", 2);
        if (parts.length != 2) {
            // 如果不是 like 查询，返回所有
            return new ArrayList<>(instsValues);
        }

        String fieldName = parts[0].trim();
        String pattern = parts[1].trim();

        // 移除 % 通配符
        String searchValue = pattern.replaceAll("^%+|%+$", "");

        return instsValues.stream()
                .filter(item -> {
                    String attrValue = item.getInstAttr().get(fieldName);
                    return attrValue != null && attrValue.contains(searchValue);
                })
                .collect(Collectors.toList());
    }

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
