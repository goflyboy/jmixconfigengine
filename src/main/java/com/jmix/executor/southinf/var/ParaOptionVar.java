package com.jmix.executor.southinf.var;

import com.jmix.executor.southinf.cp.AlgCPBoolVar;

/**
 * Stable parameter option variable facade.
 */
public class ParaOptionVar implements Var {

    private final Delegate delegate;

    public ParaOptionVar(Delegate delegate) {
        this.delegate = delegate;
    }

    public interface Delegate {
        String code();

        AlgCPBoolVar selectedVar();
    }

    public AlgCPBoolVar selected() {
        return selectedVar();
    }

    public AlgCPBoolVar selectedVar() {
        return delegate.selectedVar();
    }

    @Override
    public String code() {
        return delegate.code();
    }

    @Override
    public String name() {
        return code();
    }
}
