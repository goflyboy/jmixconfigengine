package com.jmix.executor.imodel;

/**
 * 部件类型枚举
 * 定义部件支持的类型
 * 
 * @since 2025-09-22
 */
public enum PartType {
    ATOMIC(0, "AtomicPart"),
    CATEGORY(2, "PartCategory"),
    BUNDLE(3, "Bundle"),
    GROUP(10, "Group");

    private final int value;
    private final String name;

    PartType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * 获取部件类型的数值
     * 
     * @return 部件类型的数值
     */
    public int getValue() {
        return value;
    }

    /**
     * 获取部件类型的名称
     * 
     * @return 部件类型的名称
     */
    public String getName() {
        return name;
    }

    /**
     * 根据数值获取对应的部件类型
     * 
     * @param value 部件类型的数值
     * @return 对应的部件类型枚举值
     * @throws IllegalArgumentException 当数值不匹配任何已知的部件类型时抛出
     */
    public static PartType fromValue(int value) {
        for (PartType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown part type value: " + value);
    }
}