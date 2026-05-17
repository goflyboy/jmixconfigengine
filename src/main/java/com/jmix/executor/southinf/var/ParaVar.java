package com.jmix.executor.southinf.var;

import com.jmix.executor.bmodel.para.Para;

/**
 * Stable parameter variable facade.
 */
public class ParaVar extends com.jmix.executor.impl.algmodel.Var<Para> implements Var {

    private final com.jmix.executor.impl.algmodel.ParaVar internal;

    public ParaVar(com.jmix.executor.impl.algmodel.ParaVar internal) {
        this.internal = internal;
        setBase(internal.getBase());
    }

    public com.jmix.executor.impl.algmodel.ParaVar internal() {
        return internal;
    }

    public IntExpr value() {
        return Exprs.intExpr(internal.getValue());
    }

    public BoolExpr hidden() {
        return Exprs.boolExpr(internal.getIsHidden());
    }

    public ParaOptionVar option(String optionCode) {
        return new ParaOptionVar(internal.getParaOptionByCode(optionCode));
    }

    public Integer inputValue() {
        return internal.getInputValue();
    }

    public boolean hasInput() {
        Boolean hasInputed = internal.getHasInputed();
        return Boolean.TRUE.equals(hasInputed);
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
