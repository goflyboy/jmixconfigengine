package com.jmix.executor.model;

import lombok.Data;

/**
 * 决策策略配置
 * 用于指定部件分类的变量分支优先级和排序方式
 *
 * @since 2026-04-29
 */
@Data
public class StrategyConfig {

    /** 策略类型：ASCENDING / DESCENDING / UNSPECIFIED */
    private StrategyType strategyType;

    /** 排序属性代码（如 "price"、"capacity"、"speed"） */
    private String sortAttributeCode;
}
