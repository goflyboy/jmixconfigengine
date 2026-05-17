package com.jmix.executor.southinf.cp;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * CP-SAT linear expression facade for PartCategory level operations.
 * Extends AlgCPLinearExpr to provide additional PartCategory-specific methods.
 */
public interface PartAlgCPLinearExpr extends AlgCPLinearExpr {

    @SouthApiSince(SouthApiVersion.V1_0)
    @Override
    PartAlgCPLinearExpr name(String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    @Override
    PartAlgCPLinearExpr addTerm(AlgCPIntVar var, long coefficient);

    @SouthApiSince(SouthApiVersion.V1_0)
    @Override
    PartAlgCPLinearExpr addTerm(AlgCPBoolVar var, long coefficient);

    @SouthApiSince(SouthApiVersion.V1_0)
    @Override
    PartAlgCPLinearExpr addConstant(long value);
}
