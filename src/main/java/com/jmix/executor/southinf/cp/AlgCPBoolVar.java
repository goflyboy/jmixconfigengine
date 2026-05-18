package com.jmix.executor.southinf.cp;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;
import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * CP-SAT boolean variable facade. Acts as both an integer variable (0/1) and a literal.
 */
public interface AlgCPBoolVar extends AlgCPIntVar, AlgCPLiteral {

    @SouthApiSince(SouthApiVersion.V1_0)
    @Override
    AlgCPBoolVar not();

    @SouthApiSince(SouthApiVersion.V1_0)
    BoolVar getBoolVar();

    /**
     * Convert to OR-Tools Literal for use in array-based constraints like addBoolOr, addExactlyOne.
     */
    default Literal asLiteral() {
        return getBoolVar();
    }
}
