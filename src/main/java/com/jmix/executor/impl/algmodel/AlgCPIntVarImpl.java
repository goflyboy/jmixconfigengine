package com.jmix.executor.impl.algmodel;

import com.jmix.executor.southinf.cp.AlgCPIntVar;

import com.google.ortools.sat.IntVar;

public class AlgCPIntVarImpl implements AlgCPIntVar {
    private final IntVar delegate;

    public AlgCPIntVarImpl(IntVar delegate) {
        this.delegate = delegate;
    }

    public IntVar delegate() {
        return delegate;
    }

    @Override
    public String name() {
        return delegate.getName();
    }

    @Override
    public String toString() {
        return name();
    }
}
