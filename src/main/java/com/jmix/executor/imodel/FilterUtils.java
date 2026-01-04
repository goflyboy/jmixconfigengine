package com.jmix.executor.imodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 过滤工具类
 * 提供通用的数据过滤功能
 *
 * @since 2025-12-27
 */
public final class FilterUtils {

    /**
     * 私有构造器，防止实例化
     */
    private FilterUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 根据条件查询属性映射列表
     * 支持精确匹配：fieldName = value
     * 支持模糊匹配：fieldName like %value%
     *
     * @param items         属性映射列表
     * @param whereCondition 查询条件
     * @return 匹配的属性映射列表
     */
    public static List<Map<String, String>> query(List<Map<String, String>> items, String whereCondition) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        if (whereCondition == null || whereCondition.trim().isEmpty()) {
            return new ArrayList<>(items);
        }

        String trimmedCondition = whereCondition.trim();

        // 检查是否是精确匹配查询：fieldName = value
        String[] equalParts = trimmedCondition.split("\\s*=\\s*", 2);
        if (equalParts.length == 2) {
            String fieldName = equalParts[0].trim();
            String searchValue = equalParts[1].trim();

            return items.stream()
                    .filter(item -> {
                        String attrValue = item.get(fieldName);
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

            return items.stream()
                    .filter(item -> {
                        String attrValue = item.get(fieldName);
                        return attrValue != null && attrValue.contains(searchValue);
                    })
                    .collect(Collectors.toList());
        }

        // 如果不是支持的查询格式，返回所有
        return new ArrayList<>(items);
    }

    /**
     * 根据条件查询单个属性映射
     * 如果有多个匹配项，返回第一个
     *
     * @param items         属性映射列表
     * @param whereCondition 查询条件
     * @return 匹配的属性映射，如果没有匹配则返回 null
     */
    public static Map<String, String> queryFirst(List<Map<String, String>> items, String whereCondition) {
        List<Map<String, String>> results = query(items, whereCondition);
        return results.isEmpty() ? null : results.get(0);
    }
}
