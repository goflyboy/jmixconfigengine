package com.jmix.executor.impl.artifact;

import com.google.ortools.sat.IntVar;

/**
 * 其他变量类，用于记录非ParaVar和PartVar的变量
 */
public class OtherVar {
    /**
     * 变量命名模式前缀
     */
    public static final String VAR_PATTEN_PREFIX = "v";

    /**
     * 定义的code
     */
    public String code;

    /**
     * 变量对象
     */
    public IntVar var;

    /**
     * 方便定位用的短代码
     */
    public String shortCode;

    /**
     * 构造函数
     */
    public OtherVar() {
    }

    /**
     * 构造函数
     * 
     * @param code      变量代码
     * @param var       变量对象
     * @param shortCode 短代码
     */
    public OtherVar(String code, IntVar var, String shortCode) {
        this.code = code;
        this.var = var;
        this.shortCode = shortCode;
    }

    @Override
    public String toString() {
        return String.format("OtherVar{code='%s', shortCode='%s'}", code, shortCode);
    }
}
