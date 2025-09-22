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

    protected Var() {
    }

    protected Var(T base) {
        this.base = base;
    }

    public T getBase() {
        return base;
    }

    public void setBase(T base) {
        this.base = base;
    }

    @Override
    public String getCode() {
        return base != null ? base.getCode() : null;
    }

    public String getVarString(CpSolverSolutionCallback solutionCallback) {
        return base != null ? base.getCode() : null;
    }

    public String getShortString(CpSolverSolutionCallback solutionCallback) {
        return "";
    }
}