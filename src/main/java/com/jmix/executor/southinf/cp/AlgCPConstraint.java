package com.jmix.executor.southinf.cp;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * CP-SAT constraint facade.
 */
public interface AlgCPConstraint {

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint onlyEnforceIf(AlgCPLiteral condition);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint onlyEnforceIf(AlgCPLiteral... conditions);

    @SouthApiSince(SouthApiVersion.V1_0)
    int index();
}
