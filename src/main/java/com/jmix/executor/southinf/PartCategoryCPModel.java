package com.jmix.executor.southinf;

import com.jmix.executor.southinf.cp.PartAlgCPLinearExpr;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

import java.util.List;

/**
 * CP-SAT model for PartCategory level operations.
 * Replaces IPartCategoryFunction / PartCategoryFunction.
 */
public interface PartCategoryCPModel {

    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr sum4Quantity(String attrCode, String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    com.jmix.executor.southinf.cp.AlgCPLinearExpr sum4Selected(String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    List<PartVar> partVars();

    @SouthApiSince(SouthApiVersion.V1_0)
    List<PartVar> partVars(String filterCondition);
}
