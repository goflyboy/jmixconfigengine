package com.jmix.executor.impl;

import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.model.PartConstraintReq;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private List<AggregateConditionInput> aggregateConditions = new ArrayList<>();

    /**
     * 原始部件约束请求
     */
    private PartConstraintReq orgReq;

    public List<AggregateConditionInput> getEffectiveAggregateConditions() {
        if (aggregateConditions != null && !aggregateConditions.isEmpty()) {
            return aggregateConditions;
        }
        if (comparator == null || comparator.isEmpty()
                || sumAttrCode == null || sumAttrCode.isEmpty()) {
            return Collections.emptyList();
        }
        AggregateConditionInput condition = new AggregateConditionInput();
        condition.setAttrType(attrType);
        condition.setSumAttrCode(sumAttrCode);
        condition.setComparator(comparator);
        condition.setLeftValue(leftValue);
        return Collections.singletonList(condition);
    }

    public void addAggregateCondition(AggregateConditionInput condition) {
        if (aggregateConditions == null) {
            aggregateConditions = new ArrayList<>();
        }
        aggregateConditions.add(condition);
        if (aggregateConditions.size() == 1) {
            syncSingleAggregateFields(condition);
        }
    }

    public void syncSingleAggregateFields(AggregateConditionInput condition) {
        if (condition == null) {
            return;
        }
        attrType = condition.getAttrType();
        sumAttrCode = condition.getSumAttrCode();
        comparator = condition.getComparator();
        leftValue = condition.getLeftValue();
    }
}
