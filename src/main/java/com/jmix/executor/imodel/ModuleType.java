package com.jmix.executor.imodel;

import lombok.extern.slf4j.Slf4j;

/**
 * 模块类型枚举
 * 定义模块支持的类型
 * 
 * @since 2025-09-22
 */
@Slf4j
public enum ModuleType {
    GENERAL(1, "General"),
    SET(4, "SET"),
    TEMPLATE(8, "Template"),
    TOOL(16, "Tool");

    /**
     * 模块类型的数值标识
     * 用于在数据库中存储和序列化时使用
     */
    private final int value;

    /**
     * 模块类型的显示名称
     * 用于用户界面显示和日志输出
     */
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
        log.error("Unknown module type value: {}", value);
        throw new IllegalArgumentException("Unknown module type value: " + value);
    }
}