package com.jmix.executor.southinf.view;

import java.util.List;

public interface PartCategoryInstSumView extends OntoView {
    int sumSumQuantity(); 

    PartCategoryInstView inst(int instId);

    List<PartCategoryInstView> insts();
    
    String sumDynAttr(String dynAttrKey);

    int sumDynAttr4Int(String dynAttrKey);

    List<String> dynAttrs(String dynAttrKey);

    List<Integer> dynAttrs4Int(String dynAttrKey);
}
