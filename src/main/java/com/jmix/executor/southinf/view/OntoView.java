package com.jmix.executor.southinf.view;

/**
 * Stable view over a model or instance object.
 */
public interface OntoView {

    String code();

    String extAttr(String extAttrKey);

    int extAttr4Int(String extAttrKey);

    String dynAttr(String dynAttrKey);

    int dynAttr4Int(String dynAttrKey);

    void setDynAttr(String dynAttrKey, String dynAttrValue);
}
