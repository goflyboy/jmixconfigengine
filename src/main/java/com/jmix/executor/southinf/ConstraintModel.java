package com.jmix.executor.southinf;

import java.util.Collection;

/**
 * Stable constraint model facade.
 * This interface provides the legacy expression-based API for backward compatibility.
 * New code should use {@link ModuleCPModel} directly.
 */
public interface ConstraintModel {

    // Legacy expression-based methods

    com.jmix.executor.southinf.var.ConstraintRef equal(
            com.jmix.executor.southinf.var.IntExpr left, long right);

    com.jmix.executor.southinf.var.ConstraintRef equal(
            com.jmix.executor.southinf.var.IntExpr left,
            com.jmix.executor.southinf.var.IntExpr right);

    com.jmix.executor.southinf.var.ConstraintRef greaterOrEqual(
            com.jmix.executor.southinf.var.IntExpr left, long right);

    com.jmix.executor.southinf.var.ConstraintRef greaterOrEqual(
            com.jmix.executor.southinf.var.IntExpr left,
            com.jmix.executor.southinf.var.IntExpr right);

    com.jmix.executor.southinf.var.ConstraintRef lessOrEqual(
            com.jmix.executor.southinf.var.IntExpr left, long right);

    com.jmix.executor.southinf.var.ConstraintRef implication(
            com.jmix.executor.southinf.var.BoolExpr left,
            com.jmix.executor.southinf.var.BoolExpr right);

    com.jmix.executor.southinf.var.ConstraintRef exactlyOne(
            Collection<com.jmix.executor.southinf.var.BoolExpr> expressions);

    com.jmix.executor.southinf.var.ConstraintRef compatibilityRequire(
            String ruleCode,
            com.jmix.executor.southinf.var.BoolExpr left,
            com.jmix.executor.southinf.var.BoolExpr right);

    com.jmix.executor.southinf.var.ConstraintRef compatibilityIncompatible(
            String ruleCode,
            com.jmix.executor.southinf.var.BoolExpr left,
            com.jmix.executor.southinf.var.BoolExpr right);

    com.jmix.executor.southinf.var.ConstraintRef compatibilityCoDependent(
            String ruleCode,
            com.jmix.executor.southinf.var.BoolExpr left,
            com.jmix.executor.southinf.var.BoolExpr right);

    com.jmix.executor.southinf.var.LinearExpr linearExpr(String name);

    /**
     * Create a new linear expression.
     * 
     * @param name expression name
     * @return new AlgCPLinearExpr
     */
    com.jmix.executor.impl.algmodel.AlgCPLinearExpr newLinearExpr(String name);

    void minimize(com.jmix.executor.southinf.var.LinearExpr expr);

    void maximize(com.jmix.executor.southinf.var.LinearExpr expr);
}
