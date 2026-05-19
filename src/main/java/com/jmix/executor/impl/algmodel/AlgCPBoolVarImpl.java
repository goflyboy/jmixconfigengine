package com.jmix.executor.impl.algmodel;

import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPLiteral;

import com.google.ortools.sat.BoolVar;

public class AlgCPBoolVarImpl extends AlgCPIntVarImpl implements AlgCPBoolVar {
    private final BoolVar delegate;

    public AlgCPBoolVarImpl(BoolVar delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    public BoolVar delegate() {
        return delegate;
    }

    @Override
    public AlgCPLiteral not() {
        return new AlgCPLiteralImpl(delegate.not());
    }
}
