package com.jmix.configengine.model;

/**
 * 部件类型枚举
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
    
    public int getValue() {
        return value;
    }
    
    public String getName() {
        return name;
    }
    
    public static PartType fromValue(int value) {
        for (PartType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown part type value: " + value);
    }
} 