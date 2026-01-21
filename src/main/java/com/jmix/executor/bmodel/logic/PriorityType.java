package com.jmix.executor.bmodel.logic;

/**
 * 优先级类型枚举
 * 定义优先级约束的类型
 * 
 * @since 2025-01-XX
 */
public enum PriorityType {
    /**
     * 选择性，和part.isSelected相关，例如：优先匹配高速率容量
     */
    SELECT,

    /**
     * 汇总性，和part.qty相关，例如：总容量越大越好
     */
    SUMARIZE
}
