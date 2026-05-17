package com.jmix.executor.southinf.var;

/**
 * Stable constraint reference facade.
 */
public interface ConstraintRef {

    ConstraintRef onlyIf(BoolExpr condition);

    ConstraintRef withRuleCode(String ruleCode);
}
