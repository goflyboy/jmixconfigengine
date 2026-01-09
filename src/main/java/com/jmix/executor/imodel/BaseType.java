package com.jmix.executor.imodel;

/**
 * 基础类型枚举
 * 定义动态属性的基础数据类型
 *
 * @since 2025-09-22
 */
public enum BaseType {
    INT(0, "Integer"),
    STRING(1, "String"),
    BOOL(2, "Boolean"),
    FLOAT(3, "Float"),
    DOUBLE(4, "Double");

    private final int value;
    private final String name;

    BaseType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * 获取基础类型的数值
     *
     * @return 基础类型的数值
     */
    public int getValue() {
        return value;
    }

    /**
     * 获取基础类型的名称
     *
     * @return 基础类型的名称
     */
    public String getName() {
        return name;
    }

    /**
     * 根据数值获取对应的基础类型
     *
     * @param value 基础类型的数值
     * @return 对应的基础类型枚举值
     * @throws IllegalArgumentException 当数值不匹配任何已知的基础类型时抛出
     */
    public static BaseType fromValue(int value) {
        for (BaseType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown base type value: " + value);
    }
}

