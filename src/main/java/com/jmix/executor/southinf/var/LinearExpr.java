package com.jmix.executor.southinf.var;

import com.jmix.executor.impl.algmodel.AlgCPLinearExpr;

/**
 * Stable linear expression facade.
 */
public interface LinearExpr {

    AlgCPLinearExpr unwrap();
}
