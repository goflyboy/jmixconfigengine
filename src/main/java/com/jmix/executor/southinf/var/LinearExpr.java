package com.jmix.executor.southinf.var;

import com.jmix.executor.impl.algmodel.AlgCPLinearExprImpl;

/**
 * Stable linear expression facade.
 */
public interface LinearExpr {

    AlgCPLinearExprImpl unwrap();
}
