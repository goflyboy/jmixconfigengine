package com.jmix.executor.bmodel.logic;

/**
 * 计算阶段枚举
 * 定义约束求解计算的不同阶段
 *
 * @since 2026-04-10
 */
public enum CalcStage {
    /**
     * 前置计算
     * 根据输入变量，主要是对控制参数直接赋值方式，要确保所有的控制参数是有值的（针对的输入）
     * 约束求解的赋值，输出控制变量值（是否输入）
     */
    PRE(10),

    /**
     * 中计算
     * 根据控制变量和用户的输入，使用约束求解进行计算，输出多个解
     */
    MID(20),

    /**
     * 后计算
     * 对约束求解的结果（每个解）调用后计算算法进行补充计算
     */
    POST(30);

    private final int order;

    CalcStage(int order) {
        this.order = order;
    }

    /**
     * 获取排序值
     *
     * @return 排序值
     */
    public int getOrder() {
        return order;
    }
}
