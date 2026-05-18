package com.jmix.executor.southinf.cp;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * Base interface for CP-SAT linear arguments.
 */
public interface AlgCPLinearArgument {

    @SouthApiSince(SouthApiVersion.V1_0)
    String name();

    @SouthApiSince(SouthApiVersion.V1_0)
    Object build();
}
