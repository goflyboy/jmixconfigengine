package com.jmix.executor.southinf.cp;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

@SouthApiSince(SouthApiVersion.V1_0)
public interface PartAlgCPLinearExpr extends AlgCPLinearExpr {
    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr name(String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr addExpr(PartAlgCPLinearExpr expr, long coefficient);

    @SouthApiSince(SouthApiVersion.V1_0)
    String exprStr();

    @SouthApiSince(SouthApiVersion.V1_0)
    String partTermsStr();
}
