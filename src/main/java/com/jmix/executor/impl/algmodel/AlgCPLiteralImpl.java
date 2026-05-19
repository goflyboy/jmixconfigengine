package com.jmix.executor.impl.algmodel;

import com.jmix.executor.southinf.cp.AlgCPLiteral;

import com.google.ortools.sat.Literal;

public class AlgCPLiteralImpl implements AlgCPLiteral {
    private final Literal delegate;

    public AlgCPLiteralImpl(Literal delegate) {
        this.delegate = delegate;
    }

    public Literal delegate() {
        return delegate;
    }

    @Override
    public String name() {
        return AlgCPConstraint.toNameString(delegate);
    }

    @Override
    public AlgCPLiteral not() {
        return new AlgCPLiteralImpl(delegate.not());
    }

    @Override
    public String toString() {
        return name();
    }
}
