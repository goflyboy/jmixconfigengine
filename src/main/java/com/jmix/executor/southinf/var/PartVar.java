package com.jmix.executor.southinf.var;

import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPIntVar;

/**
 * Stable part variable facade.
 */
public class PartVar implements Var {

    private final Delegate delegate;

    public PartVar(Delegate delegate) {
        this.delegate = delegate;
    }

    public interface Delegate {
        String code();

        String fatherCode();

        String attr(String attrCode);

        int attrAsInt(String attrCode);

        AlgCPIntVar quantityVar();

        AlgCPBoolVar selectedVar();

        AlgCPBoolVar hiddenVar();
    }

    public AlgCPIntVar quantity() {
        return quantityVar();
    }

    public AlgCPBoolVar selected() {
        return selectedVar();
    }

    public AlgCPBoolVar hidden() {
        return hiddenVar();
    }

    public AlgCPIntVar quantityVar() {
        return delegate.quantityVar();
    }

    public AlgCPBoolVar selectedVar() {
        return delegate.selectedVar();
    }

    public AlgCPBoolVar hiddenVar() {
        return delegate.hiddenVar();
    }

    public String fatherCode() {
        return delegate.fatherCode();
    }

    public String attr(String attrCode) {
        return delegate.attr(attrCode);
    }

    public int attrAsInt(String attrCode) {
        return delegate.attrAsInt(attrCode);
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
