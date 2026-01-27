package com.jmix.executor.bmodel.base;

/**
 * 参数赋值类型枚举
 * 定义参数的赋值来源类型
 *
 * @since 2025-09-22
 */
public enum AssignType {
    /**
     * 手工输入
     */
    INPUT(1),

    /**
     * 计算
     */
    CALC;

    private final Integer value;

    AssignType(int value) {
        this.value = value;
    }

    AssignType() {
        this.value = null;
    }

    /**
     * 获取枚举值
     *
     * @return 枚举的整数值
     */
    public int getValue() {
        return value;
    }

    /**
     * 根据整数值获取枚举
     *
     * @param value 整数值
     * @return 对应的枚举值
     */
    public static AssignType fromValue(int value) {
        for (AssignType type : AssignType.values()) {
            if (type.value != null && type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AssignType value: " + value);
    }
}
