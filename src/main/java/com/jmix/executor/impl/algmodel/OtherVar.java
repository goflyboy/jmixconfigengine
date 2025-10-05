package com.jmix.executor.impl.algmodel;

import com.google.ortools.sat.IntVar;

import lombok.Data;

/**
 * 其他变量类，用于记录非ParaVar和PartVar的变量
 * 用于存储约束求解过程中的辅助变量
 * 
 * @since 2025-09-22
 */
@Data
public class OtherVar {
    /**
     * 变量命名模式前缀
     */
    public static final String VAR_PATTERN_PREFIX = "v";

    /**
     * 定义的code
     */
    private String code;

    /**
     * 变量对象
     */
    private IntVar var;

    /**
     * 方便定位用的短代码
     */
    private String shortCode;

    /**
     * 默认构造函数
     */
    public OtherVar() {
    }

    /**
     * 带参数的构造函数
     * 
     * @param code      变量代码
     * @param var       变量对象
     * @param shortCode 短代码
     */
    public OtherVar(String code, IntVar var, String shortCode) {
        setCode(code);
        setVar(var);
        setShortCode(shortCode);
    }

    /**
     * 返回对象的字符串表示
     * 
     * @return 对象的字符串表示
     */
    @Override
    public String toString() {
        return String.format("OtherVar{code='%s', shortCode='%s'}", getCode(), getShortCode());
    }
}
