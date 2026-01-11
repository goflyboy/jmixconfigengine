package com.jmix.executor.imodel;

import com.google.ortools.sat.LinearExpr;

import lombok.Data;

/**
 * 优先级约束类
 * 用于定义优化目标的优先级约束
 * 
 * @since 2025-01-XX
 */
@Data
public class PriorityConstraint {
    /**
     * 规则代码
     */
    private String ruleCode;

    /**
     * 线性表达式
     */
    private LinearExpr expr;

    /**
     * 优先级策略
     */
    private PriorityStrategy strategy;

    /**
     * 属性代码
     */
    private String attrCode;

    /**
     * 优先级类型
     */
    private PriorityType type;
}

