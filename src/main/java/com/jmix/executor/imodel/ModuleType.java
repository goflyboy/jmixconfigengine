package com.jmix.executor.imodel;

/**
 * 模块类型枚举
 * 定义模块支持的类型
 * 
 * @since 2025-09-22
 */
public enum ModuleType {
    GENERAL(1, "General"),
    SET(4, "SET"),
    TEMPLATE(8, "Template"),
    TOOL(16, "Tool");

    private final int value;
    private final String name;

    ModuleType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * 获取模块类型的数值
     * 
     * @return 模块类型的数值
     */
    public int getValue() {
        return value;
    }

    /**
     * 获取模块类型的名称
     * 
     * @return 模块类型的名称
     */
    public String getName() {
        return name;
    }

    /**
     * 根据数值获取对应的模块类型
     * 
     * @param value 模块类型的数值
     * @return 对应的模块类型枚举值
     * @throws IllegalArgumentException 当数值不匹配任何已知的模块类型时抛出
     */
    public static ModuleType fromValue(int value) {
        for (ModuleType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown module type value: " + value);
    }
}