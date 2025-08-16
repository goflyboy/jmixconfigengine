package com.jmix.configengine.model;

/**
 * 参数类型枚举
 */
public enum ParaType {
    ENUM(0, "EnumType"),
    BOOLEAN(1, "Boolean"),
    INTEGER(2, "Integer"),
    FLOAT(3, "Float"),
    DOUBLE(4, "Double"),
    STRING(5, "String"),
    RANGE(6, "Range"),
    DATE(7, "Date"),
    MULTI_ENUM(8, "MultiEnum"),
    GROUP(10, "Group");
    
    private final int value;
    private final String name;
    
    ParaType(int value, String name) {
        this.value = value;
        this.name = name;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getName() {
        return name;
    }
    
    public static ParaType fromValue(int value) {
        for (ParaType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown para type value: " + value);
    }
} 