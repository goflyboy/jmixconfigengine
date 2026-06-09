package com.jmix.executor.impl;

import com.jmix.executor.bmodel.AttrParaType;

import lombok.Data;

/**
 * Runtime aggregate condition for a PartCategory input.
 */
@Data
public class AggregateConditionInput {

    private AttrParaType attrType = AttrParaType.Sum;

    private String sumAttrCode;

    private String comparator;

    private int leftValue;

    private boolean defaulted;
}
