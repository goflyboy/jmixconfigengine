package com.jmix.executor.southinf.view;

import com.jmix.executor.southinf.version.SouthApiSince;
import com.jmix.executor.southinf.version.SouthApiVersion;

import java.util.List;

@SouthApiSince(SouthApiVersion.V1_0)
public interface PartCategoryInstSumView extends OntoView {
    @SouthApiSince(SouthApiVersion.V1_0)
    int sumSumQuantity();

    @SouthApiSince(SouthApiVersion.V1_0)
    PartCategoryInstView inst(int instId);

    @SouthApiSince(SouthApiVersion.V1_0)
    List<PartCategoryInstView> insts();

    @SouthApiSince(SouthApiVersion.V1_0)
    String sumDynAttr(String dynAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    int sumDynAttr4Int(String dynAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    List<String> dynAttrs(String dynAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    List<Integer> dynAttrs4Int(String dynAttrKey);
}
