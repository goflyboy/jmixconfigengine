package com.jmix.executor.model;

import com.jmix.executor.bmodel.attr.DynamicAttribute;

/**
 * 部件常量属性
 * 定义部件相关的常量属性
 *
 * @since 2025-12-27
 */
public enum PartConstantAttr {

    /**
     * 数量动态属性，常量
     */
    Quantity("Quantity", DynamicAttribute.createIntegerAttr("Quantity", "配置的数量"));

    private final String code;
    private final DynamicAttribute attr;

    PartConstantAttr(String code, DynamicAttribute attr) {
        this.code = code;
        this.attr = attr;
    }

    public String getCode() {
        return code;
    }

    public DynamicAttribute getAttr() {
        return attr;
    }

    /**
     * 判断是否包含指定的值
     *
     * @param code 要检查的值
     * @return 如果包含该值返回true，否则返回false
     */
    public static boolean isContainAttrCode(String code) {
        for (PartConstantAttr attr : PartConstantAttr.values()) {
            if (attr.code.equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据代码获取对应的动态属性对象
     *
     * @param code 属性代码
     * @return 对应的DynamicAttribute对象，如果不存在则返回null
     */
    public static DynamicAttribute getAttr(String code) {
        for (PartConstantAttr constantAttr : PartConstantAttr.values()) {
            if (constantAttr.code.equals(code)) {
                return constantAttr.attr;
            }
        }
        return null;
    }
}
