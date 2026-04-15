package com.jmix.executor.impl;

import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.model.PartConstraintReq;

import lombok.Data;

@Data
public abstract class PartCategoryInputBase implements IModuleInput {
    /**
     * 获取部件分类代码
     * 
     * @return
     */
    public abstract String getPartCategoryCode();

    /**
     * 属性类型
     */
    private AttrParaType attrType = AttrParaType.Sum;

    /**
     * 求和属性代码
     */
    private String sumAttrCode;

    /**
     * 比较符
     */
    private String comparator;

    /**
     * 左值
     */
    private int leftValue;

    /**
     * 原始部件约束请求
     */
    private PartConstraintReq orgReq;
}
