package com.jmix.executor.southinf;

import com.jmix.executor.southinf.cp.AlgCPModel;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

@SouthApiSince(SouthApiVersion.V1_0)
public interface ModuleCPModel extends AlgCPModel, PartCategoryCPModel {
    @SouthApiSince(SouthApiVersion.V1_0)
    ParaVar para(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartVar part(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartCategoryVar partCategory(String code);
}
