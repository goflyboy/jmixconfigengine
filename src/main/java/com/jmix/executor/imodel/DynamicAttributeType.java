package com.jmix.executor.imodel;

/**
 * 动态属性类型枚举
 * 定义动态属性支持的数据类型
 * YXX --XX是baseType,Y是CompositeType
 *
 * @since 2025-09-22
 */
public enum DynamicAttributeType {
    E_INT(0, "ENUM Integer"), // 对应原有的Integer
    E_STRING(1, "ENUM String"),
    E_BOOL(2, "ENUM Boolean"),
    E_FLOAT(3, "ENUM Float"),
    E_DOUBLE(4, "ENUM Double"),
    B_INT(100, "base Integer"),
    B_STRING(101, "base String"),
    B_BOOL(102, "base Boolean"),
    B_FLOAT(103, "base Float"),
    B_DOUBLE(104, "base Double");

    /**
     * BASE值的常量
     */
    public static final int BASE_VALUE = 100;

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
     * 获取组合类型
     *
     * @return 组合类型
     */
    public CompositeType getCompositeType() {
        return CompositeType.fromValue(this.value / BASE_VALUE);
    }

    /**
     * 获取基础类型
     *
     * @return 基础类型
     */
    public BaseType getBaseType() {
        return BaseType.fromValue(this.value % BASE_VALUE);
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

    /**
     * 基础类型枚举
     */
    public enum BaseType {
        INT(0, "Integer"),
        STRING(1, "String"),
        BOOL(2, "Boolean"),
        FLOAT(3, "Float"),
        DOUBLE(4, "Double");

        private final int value;
        private final String name;

        BaseType(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }

        public static BaseType fromValue(int value) {
            for (BaseType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown base type value: " + value);
        }
    }

    /**
     * 组合类型枚举
     */
    public enum CompositeType {
        ENUM(0, "EnumType"),
        BASE(1, "Base"),
        RANGE(2, "Range");

        private final int value;
        private final String name;

        CompositeType(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }

        public static CompositeType fromValue(int value) {
            for (CompositeType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown composite type value: " + value);
        }
    }
}
