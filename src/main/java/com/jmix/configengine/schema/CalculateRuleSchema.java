package com.jmix.configengine.schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 计算规则Schema
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CalculateRuleSchema extends RuleSchema {
    /**
     * 左表达式
     */
    private ExprSchema leftExpr;
} 