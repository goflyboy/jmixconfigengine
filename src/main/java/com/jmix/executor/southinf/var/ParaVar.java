package com.jmix.executor.southinf.var;

import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPIntVar;

/**
 * Stable parameter variable facade.
 */
public class ParaVar implements Var {

    private final Delegate delegate;

    public ParaVar(Delegate delegate) {
        this.delegate = delegate;
    }

    public interface Delegate {
        String code();

        AlgCPIntVar valueVar();

        AlgCPBoolVar hiddenVar();

        ParaOptionVar option(String optionCode);

        Integer inputValue();

        boolean hasInput();
    }

    public AlgCPIntVar value() {
        return valueVar();
    }

    public AlgCPBoolVar hidden() {
        return hiddenVar();
    }

    public AlgCPIntVar valueVar() {
        return delegate.valueVar();
    }

    public AlgCPBoolVar hiddenVar() {
        return delegate.hiddenVar();
    }

    public ParaOptionVar option(String optionCode) {
        return delegate.option(optionCode);
    }

    public Integer inputValue() {
        return delegate.inputValue();
    }

    public boolean hasInput() {
        return delegate.hasInput();
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
