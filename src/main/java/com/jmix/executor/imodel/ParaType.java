package com.jmix.executor.imodel;

import lombok.extern.slf4j.Slf4j;

/**
 * 参数类型枚举
 * 定义参数支持的数据类型
 *
 * @since 2025-09-22
 */
@Slf4j
public enum ParaType { //YXX --XX是subType,mainType
    ENUM(0, "EnumType"),//Y=0, XX=0
    BOOLEAN(1, "Boolean"),//Y=0, XX=0
    INTEGER(2, "Integer"),//Y=0, XX=0
    FLOAT(3, "Float"),//Y=0, XX=0
    DOUBLE(4, "Double"),//Y=0, XX=0
    STRING(5, "String"),//Y=0, XX=0
    RANGE(6, "Range"),//Y=0, XX=0
    DATE(7, "Date"),//Y=0, XX=0
    MULTI_ENUM(8, "MultiEnum"),//Y=0, XX=0
    GROUP(100, "Group");//Y=1, XX=00(STRING)

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

    public static int BASE_TYPE = 100;

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

    int getSubType() {
        return this.value % BASE_TYPE;
    }

    ParaMainType getMainType() {
        return ParaMainType.fromValue(this.value / BASE_TYPE);
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

    public enum ParaMainType{
        ATOMIC(0, "AtomicPara"), //原子para,默认是原子Para
        GROUP(1, "Group");//类似中间件层

        private final int value;
        private final String name;

        ParaMainType(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }

        public static ParaMainType fromValue(int value) {
            for (ParaMainType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            log.error("Unknown para main type value: {}", value);
            throw new IllegalArgumentException("Unknown para main type value: " + value);
        }
    }
}