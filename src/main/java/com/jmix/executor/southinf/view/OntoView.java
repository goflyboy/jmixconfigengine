package com.jmix.executor.southinf.view;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

/**
 * Stable view over a model or instance object.
 */
@SouthApiSince(SouthApiVersion.V1_0)
public interface OntoView {

    @SouthApiSince(SouthApiVersion.V1_0)
    String code();

    @SouthApiSince(SouthApiVersion.V1_0)
    String extAttr(String extAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    int extAttr4Int(String extAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    String dynAttr(String dynAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    int dynAttr4Int(String dynAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    void setDynAttr(String dynAttrKey, String dynAttrValue);
}
