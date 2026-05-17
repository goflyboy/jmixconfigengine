package com.jmix.executor.southinf.cp;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * CP-SAT boolean variable facade. Acts as both an integer variable (0/1) and a literal.
 */
public interface AlgCPBoolVar extends AlgCPIntVar, AlgCPLiteral {

    @SouthApiSince(SouthApiVersion.V1_0)
    @Override
    AlgCPBoolVar not();
}
