package com.jmix.executor.southinf.var;

import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.impl.algmodel.ParaVarImpl;

/**
 * Stable parameter variable facade.
 */
public class ParaVar extends SouthboundVarAdapter<Para> implements Var {

    private final ParaVarImpl internal;

    public ParaVar(ParaVarImpl internal) {
        this.internal = internal;
        setBase(internal.getBase());
    }

    public ParaVarImpl internal() {
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
