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
     * 支持精确匹配：fieldName = value，例如：Capacity =8T
     * 支持模糊匹配：fieldName like %value%，例如：Speed like %5400%
     *
     * @param whereCondition 查询条件表达式
     * @return 匹配的实例属性值列表
     */
    public List<InstanceDynAttrValueItem> query(String whereCondition) {
        if (whereCondition == null || whereCondition.trim().isEmpty()) {
            return new ArrayList<>(instsValues);
        }

        String trimmedCondition = whereCondition.trim();

        // 检查是否是精确匹配查询：fieldName = value
        String[] equalParts = trimmedCondition.split("\\s*=\\s*", 2);
        if (equalParts.length == 2) {
            String fieldName = equalParts[0].trim();
            String searchValue = equalParts[1].trim();

            return instsValues.stream()
                    .filter(item -> {
                        String attrValue = item.getInstAttr().get(fieldName);
                        return attrValue != null && attrValue.equals(searchValue);
                    })
                    .collect(Collectors.toList());
        }

        // 检查是否是模糊匹配查询：fieldName like %value%
        String[] likeParts = trimmedCondition.split("\\s+like\\s+", 2);
        if (likeParts.length == 2) {
            String fieldName = likeParts[0].trim();
            String pattern = likeParts[1].trim();

            // 移除 % 通配符
            String searchValue = pattern.replaceAll("^%+|%+$", "");

            return instsValues.stream()
                    .filter(item -> {
                        String attrValue = item.getInstAttr().get(fieldName);
                        return attrValue != null && attrValue.contains(searchValue);
                    })
                    .collect(Collectors.toList());
        }

        // 如果不是支持的查询格式，返回所有
        return new ArrayList<>(instsValues);
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
