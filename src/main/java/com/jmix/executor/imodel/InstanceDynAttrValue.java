package com.jmix.executor.imodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        // 将实例属性值转换为属性映射列表
        List<Map<String, String>> attrMaps = instsValues.stream()
                .map(InstanceDynAttrValueItem::getInstAttr)
                .collect(Collectors.toList());

        // 使用工具类进行查询
        List<Map<String, String>> filteredAttrs = FilterUtils.query(attrMaps, whereCondition);

        // 将过滤后的属性映射转换回实例属性值对象
        return instsValues.stream()
                .filter(item -> filteredAttrs.contains(item.getInstAttr()))
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
