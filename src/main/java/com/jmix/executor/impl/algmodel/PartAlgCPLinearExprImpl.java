package com.jmix.executor.impl.algmodel;

public class PartAlgCPLinearExprImpl extends AlgCPLinearExprImpl
        implements com.jmix.executor.southinf.cp.PartAlgCPLinearExpr {
    private final com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr delegate;

    public PartAlgCPLinearExprImpl(String name) {
        this(new com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr(name));
    }

    public PartAlgCPLinearExprImpl(com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    public com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr delegate() {
        return delegate;
    }

    @Override
    public com.jmix.executor.southinf.cp.PartAlgCPLinearExpr name(String name) {
        delegate.setName(name);
        return this;
    }

    @Override
    public com.jmix.executor.southinf.cp.PartAlgCPLinearExpr addExpr(
            com.jmix.executor.southinf.cp.PartAlgCPLinearExpr expr, long coefficient) {
        delegate.addExpr(AlgCPFacadeAdapters.unwrapPartExpr(expr), coefficient);
        return this;
    }

    @Override
    public String exprStr() {
        return delegate.getExprStr();
    }

    @Override
    public String partTermsStr() {
        return delegate.getPartTermsStr();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
