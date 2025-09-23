package com.jmix.executor.impl.algmodel;

import com.jmix.executor.imodel.Programmable;

import com.google.ortools.sat.CpSolverSolutionCallback;

/**
 * 通用变量包装器，绑定到可编程模型对象
 * 所有变量类型的基类
 * 
 * @param <T> 可编程对象类型
 * @since 2025-09-22
 */
public abstract class Var<T extends Programmable> implements Programmable {
    protected T base;

    /**
     * 默认构造函数
     */
    protected Var() {
    }

    /**
     * 带基础对象的构造函数
     * 
     * @param base 基础对象
     */
    protected Var(T base) {
        this.base = base;
    }

    /**
     * 获取基础对象
     * 
     * @return 基础对象
     */
    public T getBase() {
        return base;
    }

    /**
     * 设置基础对象
     * 
     * @param base 基础对象
     */
    public void setBase(T base) {
        this.base = base;
    }

    @Override
    public String getCode() {
        return base != null ? base.getCode() : null;
    }

    /**
     * 获取变量的字符串表示
     * 
     * @param solutionCallback 求解器回调
     * @return 变量的字符串表示
     */
    public String getVarString(CpSolverSolutionCallback solutionCallback) {
        return base != null ? base.getCode() : "";
    }

    /**
     * 获取变量的简短字符串表示
     * 
     * @param solutionCallback 求解器回调
     * @return 变量的简短字符串表示
     */
    public String getShortString(CpSolverSolutionCallback solutionCallback) {
        return "";
    }
}