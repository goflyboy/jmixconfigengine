package com.jmix.configengine.model;

/**
 * 模块类型枚举
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
    
    public int getValue() {
        return value;
    }
    
    public String getName() {
        return name;
    }
    
    public static ModuleType fromValue(int value) {
        for (ModuleType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown module type value: " + value);
    }
} 