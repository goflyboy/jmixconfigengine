package com.jmix.executor.impl.algmodel;

import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPIntVar;

public class AlgCPLinearExprImpl implements com.jmix.executor.southinf.cp.AlgCPLinearExpr {
    private final AlgCPLinearExpr delegate;

    public AlgCPLinearExprImpl(String name) {
        this(new AlgCPLinearExpr(name));
    }

    public AlgCPLinearExprImpl(AlgCPLinearExpr delegate) {
        this.delegate = delegate;
    }

    public AlgCPLinearExpr delegate() {
        return delegate;
    }

    @Override
    public String name() {
        return delegate.getName();
    }

    @Override
    public com.jmix.executor.southinf.cp.AlgCPLinearExpr name(String name) {
        delegate.setName(name);
        return this;
    }

    @Override
    public com.jmix.executor.southinf.cp.AlgCPLinearExpr addTerm(AlgCPIntVar var, long coefficient) {
        delegate.addTerm(AlgCPFacadeAdapters.unwrapInt(var), coefficient);
        return this;
    }

    @Override
    public com.jmix.executor.southinf.cp.AlgCPLinearExpr addTerm(AlgCPBoolVar var, long coefficient) {
        delegate.addTerm(AlgCPFacadeAdapters.unwrapBool(var), coefficient);
        return this;
    }

    @Override
    public com.jmix.executor.southinf.cp.AlgCPLinearExpr addConstant(long value) {
        delegate.addConstant(value);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
