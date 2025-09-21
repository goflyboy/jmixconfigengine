package com.jmix.executor.impl.artifact;

import com.jmix.executor.imodel.Programmable;

import com.google.ortools.sat.CpSolverSolutionCallback;

/**
 * Generic variable wrapper that binds to a programmable model object
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