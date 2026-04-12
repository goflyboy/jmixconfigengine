package com.jmix.executor.bmodel.logic;

/**
 * 基数枚举
 * 定义规则中编程对象的基数
 * 
 * @since 2026-04-12
 */
public enum Cardinality {
    /**
     * 单个基数（1:1，N:1中的1）
     */
    ONE(10),
    
    /**
     * 多个基数（1:N，M:N中的N/*）
     */
    MANY(20);

    private final int code;

    Cardinality(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}