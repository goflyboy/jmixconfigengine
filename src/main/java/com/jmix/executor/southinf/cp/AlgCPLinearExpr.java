package com.jmix.executor.southinf.cp;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * CP-SAT linear expression facade for building linear constraints.
 */
public interface AlgCPLinearExpr extends AlgCPLinearArgument {

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr name(String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr addTerm(AlgCPIntVar var, long coefficient);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr addTerm(AlgCPBoolVar var, long coefficient);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr addConstant(long value);

    @SouthApiSince(SouthApiVersion.V1_0)
    boolean isEmpty();

    @SouthApiSince(SouthApiVersion.V1_0)
    Object build();
}
