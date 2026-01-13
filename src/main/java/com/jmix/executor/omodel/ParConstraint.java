package com.jmix.executor.omodel;

import lombok.Data;

/**
 * 部件约束数据类
 * 用于存储部件约束的相关信息
 *
 * @since 2025-01-XX
 */
@Data
public class ParConstraint {
    /**
     * 求和属性代码
     */
    private String sumAttrCode;

    /**
     * 比较符
     */
    private String comparator;

    /**
     * 左值
     */
    private int leftValue;
}

