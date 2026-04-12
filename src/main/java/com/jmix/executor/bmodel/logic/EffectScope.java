package com.jmix.executor.bmodel.logic;

/**
 * 作用范围枚举
 * 定义规则的作用范围
 * 
 * @since 2026-04-12
 */
public enum EffectScope {
    /**
     * 本部件分类的每个单实例
     */
    SingleInst(10),
    
    /**
     * 本部件分类的所有部件实例
     */
    AllInst(20);

    private final int code;

    EffectScope(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}