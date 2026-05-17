package com.jmix.executor.southinf.var;

import com.google.ortools.sat.Literal;

/**
 * Stable boolean expression facade.
 */
public interface BoolExpr extends IntExpr {

    Literal literal();
}
