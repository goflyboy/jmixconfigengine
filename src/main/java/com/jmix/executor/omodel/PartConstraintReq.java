package com.jmix.executor.omodel;

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
     * 部件分类代码
     */
    private String partCategory;

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
    private String partCatagoryCode;
}
