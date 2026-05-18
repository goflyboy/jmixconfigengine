package com.jmix.executor.southinf.var;

import com.google.ortools.sat.Literal;

/**
 * Stable constraint reference facade.
 */
public interface ConstraintRef {

    ConstraintRef onlyIf(BoolExpr condition);

    ConstraintRef onlyEnforceIf(Literal condition);

    ConstraintRef withRuleCode(String ruleCode);
}
