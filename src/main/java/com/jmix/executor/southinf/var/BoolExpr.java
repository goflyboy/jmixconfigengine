package com.jmix.executor.southinf.var;

/**
 * Stable boolean expression facade.
 */
public interface BoolExpr extends IntExpr {

    com.google.ortools.sat.Literal literal();
}
