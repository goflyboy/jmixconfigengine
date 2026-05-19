package com.jmix.executor.impl.algmodel;

import com.jmix.executor.southinf.cp.AlgCPConstraint;
import com.jmix.executor.southinf.cp.AlgCPLiteral;

public class AlgCPConstraintImpl implements AlgCPConstraint {
    private final com.jmix.executor.impl.algmodel.AlgCPConstraint delegate;

    public AlgCPConstraintImpl(com.jmix.executor.impl.algmodel.AlgCPConstraint delegate) {
        this.delegate = delegate;
    }

    public com.jmix.executor.impl.algmodel.AlgCPConstraint delegate() {
        return delegate;
    }

    @Override
    public AlgCPConstraint onlyEnforceIf(AlgCPLiteral condition) {
        delegate.onlyEnforceIf(AlgCPFacadeAdapters.unwrapLiteral(condition));
        return this;
    }

    @Override
    public AlgCPConstraint onlyEnforceIf(AlgCPLiteral... conditions) {
        for (AlgCPLiteral condition : conditions) {
            onlyEnforceIf(condition);
        }
        return this;
    }

    @Override
    public int index() {
        return delegate.getIndex();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
