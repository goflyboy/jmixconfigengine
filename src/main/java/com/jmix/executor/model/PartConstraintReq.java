package com.jmix.executor.model;

import lombok.Data;

/**
 * 部件约束请求
 * 用于定义部件查询的约束条件
 *
 * @since 2025-12-27
 */
@Data
public class PartConstraintReq {

    /**
     * 属性代码
     */
    private String attrCode;

    /**
     * 属性比较符
     */
    private String attrComparator;

    /**
     * 属性值
     */
    private String attrValue;

    /**
     * 属性过滤条件
     */
    private String attrWhereCondition;

    /**
     * 部件分类代码（用于查询时指定具体的PartCategory）
     */
    private String partCategoryCode;

    /**
     * 生成短字符串表示
     * 格式：partCategoryCode:attrCode attrComparator attrValue where
     * attrWhereCondition
     * 示例："drive:sum.Capacity >=5 where Speed like %5400%"
     * 
     * @return 短字符串表示
     */
    public String toShortString() {
        StringBuilder sb = new StringBuilder();

        // 添加部件分类代码
        if (partCategoryCode != null && !partCategoryCode.isEmpty()) {
            sb.append(partCategoryCode).append(":");
        }

        // 添加属性代码、比较符和值
        if (attrCode != null && !attrCode.isEmpty()) {
            sb.append(attrCode);
            if (attrComparator != null && !attrComparator.isEmpty() && attrValue != null && !attrValue.isEmpty()) {
                sb.append(" ").append(attrComparator);
                // 比较符和值之间不加空格，例如 ">=5" 而不是 ">= 5"
                sb.append(attrValue);
            }
        }

        // 添加 where 条件
        if (attrWhereCondition != null && !attrWhereCondition.isEmpty()) {
            sb.append(" where ").append(attrWhereCondition);
        }

        return sb.toString();
    }
}
