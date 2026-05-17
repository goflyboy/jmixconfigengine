package com.jmix.executor.southinf.var;

import com.jmix.executor.bmodel.IPart;
import com.jmix.executor.impl.algmodel.PartVarImpl;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;

/**
 * Stable part variable facade.
 */
public class PartVar extends SouthboundVarAdapter<IPart> implements Var {

    private final PartVarImpl internal;

    public PartVar(PartVarImpl internal) {
        this.internal = internal;
        setBase(internal.getBase());
    }

    public PartVarImpl internal() {
        return internal;
    }

    public IntExpr quantity() {
        return Exprs.intExpr(internal.getQty());
    }

    public BoolExpr selected() {
        return Exprs.boolExpr(internal.getIsSelected());
    }

    public BoolExpr hidden() {
        return Exprs.boolExpr(internal.getIsHidden());
    }

    public IntVar quantityVar() {
        return internal.getQty();
    }

    public BoolVar selectedVar() {
        return internal.getIsSelected();
    }

    public BoolVar hiddenVar() {
        return internal.getIsHidden();
    }

    public String fatherCode() {
        return internal.getBase().getFatherCode();
    }

    public String attr(String attrCode) {
        return internal.getAttr(attrCode);
    }

    public int attrAsInt(String attrCode) {
        return internal.getAttr4Int(attrCode);
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
