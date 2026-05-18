package com.jmix.executor.southinf.var;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * Stable variable facade.
 */
@SouthApiSince(SouthApiVersion.V1_0)
public interface Var {

    @SouthApiSince(SouthApiVersion.V1_0)
    String code();

    @SouthApiSince(SouthApiVersion.V1_0)
    String name();
}
