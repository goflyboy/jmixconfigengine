package com.jmix.executor.southinf.var;

import com.google.ortools.sat.LinearArgument;

/**
 * Stable integer expression facade.
 */
public interface IntExpr {

    LinearArgument unwrap();
}
