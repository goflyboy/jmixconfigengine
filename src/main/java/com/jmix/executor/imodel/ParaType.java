package com.jmix.executor.imodel;

import lombok.extern.slf4j.Slf4j;

/**
 * 参数类型枚举
 * 定义参数支持的数据类型
 *
 * @since 2025-09-22
 */
@Slf4j
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
    ATOMIC(9, "AtomicPara"),
    GROUP(10, "Group");//类似中间件层

    /**
     * 参数类型的数值标识
     * 用于在数据库中存储和序列化时使用
     */
    private final int value;

    /**
     * 参数类型的显示名称
     * 用于用户界面显示和日志输出
     */
    private final String name;

    ParaType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * 获取参数类型的数值
     * 
     * @return 参数类型的数值
     */
    public int getValue() {
        return value;
    }

    /**
     * 获取参数类型的名称
     * 
     * @return 参数类型的名称
     */
    public String getName() {
        return name;
    }

    /**
     * 根据value获取ParaType
     * 
     * @param value 参数类型的数值
     * @return 对应的ParaType枚举值
     * @throws IllegalArgumentException 当value不匹配任何枚举值时
     */
    public static ParaType fromValue(int value) {
        for (ParaType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        log.error("Unknown para type value: {}", value);
        throw new IllegalArgumentException("Unknown para type value: " + value);
    }
}