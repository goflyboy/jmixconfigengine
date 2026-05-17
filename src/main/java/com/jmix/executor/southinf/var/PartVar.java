package com.jmix.executor.southinf.var;

import com.jmix.executor.bmodel.IPart;

/**
 * Stable part variable facade.
 */
public class PartVar extends com.jmix.executor.impl.algmodel.Var<IPart> implements Var {

    private final com.jmix.executor.impl.algmodel.PartVar internal;

    public PartVar(com.jmix.executor.impl.algmodel.PartVar internal) {
        this.internal = internal;
        setBase(internal.getBase());
    }

    public com.jmix.executor.impl.algmodel.PartVar internal() {
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
