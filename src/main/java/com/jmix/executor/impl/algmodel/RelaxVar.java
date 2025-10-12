package com.jmix.executor.impl.algmodel;

import com.google.ortools.sat.BoolVar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 松弛变量类，用于约束松弛和冲突检测
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelaxVar {
    // 权重常量
    public static final int WEIGHT_MEDIUM = 25;

    public static final int WEIGHT_BIG = 1000;

    public static final int WEIGHT_SMALL = 1; // 一般系统级的约束设置为SMALL

    public static final int WEIGHT_ADDER = 1000; // 一般系统级的约束设置为SMALL

    /**
     * 松弛变量名称
     */
    private String name;

    /**
     * 规则代码
     */
    private String ruleCode;

    /**
     * 布尔变量值
     */
    private BoolVar value;

    /**
     * 权重
     */
    private int weight;
}
