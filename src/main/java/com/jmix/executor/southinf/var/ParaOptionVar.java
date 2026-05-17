package com.jmix.executor.southinf.var;

/**
 * Stable parameter option variable facade.
 */
public class ParaOptionVar extends com.jmix.executor.impl.algmodel.Var<com.jmix.executor.bmodel.attr.DynamicAttributerOption>
        implements Var {

    private final com.jmix.executor.impl.algmodel.ParaOptionVar internal;

    public ParaOptionVar(com.jmix.executor.impl.algmodel.ParaOptionVar internal) {
        this.internal = internal;
        setBase(internal.getBase());
    }

    public com.jmix.executor.impl.algmodel.ParaOptionVar internal() {
        return internal;
    }

    public BoolExpr selected() {
        return Exprs.boolExpr(internal.getIsSelectedVar());
    }

    @Override
    public String code() {
        return internal.getCode();
    }

    @Override
    public String name() {
        return code();
    }
}
