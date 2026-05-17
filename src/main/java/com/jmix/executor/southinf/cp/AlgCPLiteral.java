package com.jmix.executor.southinf.cp;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * CP-SAT literal facade. Represents a boolean variable or its negation.
 */
public interface AlgCPLiteral extends AlgCPLinearArgument {

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLiteral not();

    @SouthApiSince(SouthApiVersion.V1_0)
    boolean isNegated();

    @SouthApiSince(SouthApiVersion.V1_0)
    Object internal();
}
