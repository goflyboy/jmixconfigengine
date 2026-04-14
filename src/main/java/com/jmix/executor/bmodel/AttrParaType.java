package com.jmix.executor.bmodel;

import lombok.ToString;

/**
 * 属性参数类型枚举
 * 
 * @since 2026-04-13
 */
@ToString
public enum AttrParaType {

    /**
     * 对每个分类实例的汇总（考虑单选和多选）
     */
    Sum(10),

    /**
     * 对每个分类所有的实例值汇总，对应输入多实例
     */
    SumSum(20),

    /**
     * 某个部件原始的值，不是汇总的
     */
    Org(30);

    private final int code;

    AttrParaType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 根据编码获取枚举值
     * 
     * @param code 编码
     * @return 对应的枚举值，如果未找到则返回null
     */
    public static AttrParaType fromCode(int code) {
        for (AttrParaType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}