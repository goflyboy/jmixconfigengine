package com.jmix.configengine.schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 兼容规则Schema
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompatiableRuleSchema extends RuleSchema {
    /**
     * 左表达式
     */
    private ExprSchema leftExpr;
    
    /**
     * 操作符：Incompatible, CoRefent, Requires
     */
    private String operator;
    
    /**
     * 右表达式
     */
    private ExprSchema rightExpr;
} 