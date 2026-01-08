package com.jmix.executor.imodel;

/**
 * 动态属性类型枚举
 * 定义动态属性支持的数据类型
 *
 * @since 2025-09-22
 */
public enum DynamicAttributeType {
    Integer(0, "Integer"),
    String(1, "String"),
    Boolean(2, "Boolean"),
    Float(3, "Float"),
    Double(4, "Double");

    /**
     * 动态属性类型的数值标识
     */
    private final int value;

    /**
     * 动态属性类型的显示名称
     */
    private final String name;

    DynamicAttributeType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * 获取动态属性类型的数值
     *
     * @return 动态属性类型的数值
     */
    public int getValue() {
        return value;
    }

    /**
     * 获取动态属性类型的名称
     *
     * @return 动态属性类型的名称
     */
    public String getName() {
        return name;
    }

    /**
     * 根据数值获取对应的动态属性类型
     *
     * @param value 动态属性类型的数值
     * @return 对应的动态属性类型枚举值
     * @throws IllegalArgumentException 当数值不匹配任何已知的动态属性类型时抛出
     */
    public static DynamicAttributeType fromValue(int value) {
        for (DynamicAttributeType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown dynamic attribute type value: " + value);
    }
}
