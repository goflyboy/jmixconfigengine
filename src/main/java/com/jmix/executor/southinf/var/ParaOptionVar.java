package com.jmix.executor.southinf.var;

import com.jmix.executor.bmodel.attr.DynamicAttributerOption;
import com.jmix.executor.impl.algmodel.ParaOptionVarImpl;

import com.google.ortools.sat.BoolVar;

/**
 * Stable parameter option variable facade.
 */
public class ParaOptionVar extends SouthboundVarAdapter<DynamicAttributerOption> implements Var {

    private final ParaOptionVarImpl internal;

    public ParaOptionVar(ParaOptionVarImpl internal) {
        this.internal = internal;
        setBase(internal.getBase());
    }

    public ParaOptionVarImpl internal() {
        return internal;
    }

    public BoolExpr selected() {
        return Exprs.boolExpr(internal.getIsSelectedVar());
    }

    public BoolVar selectedVar() {
        return internal.getIsSelectedVar();
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
