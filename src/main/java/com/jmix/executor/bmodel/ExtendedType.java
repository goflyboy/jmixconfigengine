package com.jmix.executor.bmodel;

/**
 * 扩展类型枚举
 * 定义动态属性的扩展类型（原CompositeType）
 *
 * @since 2025-09-22
 */
public enum ExtendedType {
    ENUM(0, "EnumType"),
    BASE(1, "Base"),
    RANGE(2, "Range");

    private final int value;
    private final String name;

    ExtendedType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * 获取扩展类型的数值
     *
     * @return 扩展类型的数值
     */
    public int getValue() {
        return value;
    }

    /**
     * 获取扩展类型的名称
     *
     * @return 扩展类型的名称
     */
    public String getName() {
        return name;
    }

    /**
     * 根据数值获取对应的扩展类型
     *
     * @param value 扩展类型的数值
     * @return 对应的扩展类型枚举值
     * @throws IllegalArgumentException 当数值不匹配任何已知的扩展类型时抛出
     */
    public static ExtendedType fromValue(int value) {
        for (ExtendedType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown extended type value: " + value);
    }
}
