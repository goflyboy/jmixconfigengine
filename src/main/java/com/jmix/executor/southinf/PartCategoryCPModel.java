package com.jmix.executor.southinf;

import com.jmix.executor.southinf.cp.AlgCPLinearExpr;
import com.jmix.executor.southinf.cp.PartAlgCPLinearExpr;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

import java.util.List;

@SouthApiSince(SouthApiVersion.V1_0)
public interface PartCategoryCPModel {
    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr sum4Quantity(String attrCode, String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr sum4Quantity(String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr sum4Quantity(String partCategoryCodes, String attrCode, String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr sum4Selected(String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr sum4Selected(String attrCode, String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr sum4Selected(String partCategoryCodes, String attrCode, String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    List<PartVar> partVars();

    @SouthApiSince(SouthApiVersion.V1_0)
    List<PartVar> partVars(String filterCondition);
}
