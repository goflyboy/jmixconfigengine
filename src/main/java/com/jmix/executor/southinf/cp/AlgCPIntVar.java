package com.jmix.executor.southinf.cp;

import com.google.ortools.sat.IntVar;
import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * CP-SAT integer variable facade.
 */
public interface AlgCPIntVar extends AlgCPLinearArgument {

    @SouthApiSince(SouthApiVersion.V1_0)
    long lowerBound();

    @SouthApiSince(SouthApiVersion.V1_0)
    long upperBound();

    @SouthApiSince(SouthApiVersion.V1_0)
    IntVar getIntVar();
}
