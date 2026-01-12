package com.jmix.executor.imodel;

import com.jmix.executor.imodel.rule.PriorityRuleSchema;

import lombok.Data;

/**
 * 优先级属性最优值对象
 * 用于存储优先级规则的最优值信息
 * 
 * @since 2025-01-XX
 */
@Data
public class PriorityAttrObjValue {
    /**
     * 属性代码
     */
    private String attrCode;

    /**
     * 优先级规则Schema
     */
    private PriorityRuleSchema schema;

    /**
     * 最优值
     */
    private double optimalValue;
}

