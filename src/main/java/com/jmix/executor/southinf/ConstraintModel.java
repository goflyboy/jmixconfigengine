package com.jmix.executor.southinf;

import com.jmix.executor.southinf.var.BoolExpr;
import com.jmix.executor.southinf.var.ConstraintRef;
import com.jmix.executor.southinf.var.IntExpr;
import com.jmix.executor.southinf.var.LinearExpr;

import java.util.Collection;

/**
 * Stable constraint model facade.
 */
public interface ConstraintModel {

    ConstraintRef equal(IntExpr left, long right);

    ConstraintRef equal(IntExpr left, IntExpr right);

    ConstraintRef greaterOrEqual(IntExpr left, long right);

    ConstraintRef greaterOrEqual(IntExpr left, IntExpr right);

    ConstraintRef lessOrEqual(IntExpr left, long right);

    ConstraintRef implication(BoolExpr left, BoolExpr right);

    ConstraintRef exactlyOne(Collection<BoolExpr> expressions);

    ConstraintRef compatibilityRequire(String ruleCode, BoolExpr left, BoolExpr right);

    ConstraintRef compatibilityIncompatible(String ruleCode, BoolExpr left, BoolExpr right);

    ConstraintRef compatibilityCoDependent(String ruleCode, BoolExpr left, BoolExpr right);

    LinearExpr linearExpr(String name);

    void minimize(LinearExpr expr);

    void maximize(LinearExpr expr);
}
