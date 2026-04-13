package com.jmix.executor.bmodel;

/**
 * 属性参数类型枚举
 * 
 * @since 2026-04-13
 */
public enum AttrParaType {
    /**
     * 对每个分类实例的汇总（考虑单选和多选）
     */
    SUM(10),
    
    /**
     * 对每个分类所有的实例值汇总，对应输入多实例
     */
    SUMSUM(20);

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