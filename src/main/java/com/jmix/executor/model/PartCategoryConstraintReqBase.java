package com.jmix.executor.model;

import com.jmix.executor.bmodel.AttrParaType;

import lombok.Data;

import java.util.List;

/**
 * Base request fields shared by PartCategory input constraints.
 */
@Data
public abstract class PartCategoryConstraintReqBase {

    /**
     * 属性类型
     */
    private AttrParaType attrType = AttrParaType.Sum;

    /**
     * 属性代码
     */
    private String attrCode;

    /**
     * 属性比较符
     */
    private String attrComparator;

    /**
     * 属性值
     */
    private String attrValue;

    /**
     * 属性过滤条件
     */
    private String attrWhereCondition;

    /**
     * 决策策略列表
     */
    private List<StrategyConfig> decisionStrategies;
}
