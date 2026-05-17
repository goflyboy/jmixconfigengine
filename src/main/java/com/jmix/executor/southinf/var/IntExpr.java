package com.jmix.executor.southinf.var;

/**
 * Stable integer expression facade.
 */
public interface IntExpr {

    com.google.ortools.sat.LinearArgument unwrap();
}
