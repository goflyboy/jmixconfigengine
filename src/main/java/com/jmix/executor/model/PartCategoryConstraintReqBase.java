package com.jmix.executor.model;

import com.jmix.executor.bmodel.AttrParaType;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
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

    private List<AggregateConditionReq> aggregateConditions = new ArrayList<>();

    /**
     * 决策策略列表
     */
    private List<StrategyConfig> decisionStrategies;

    public List<AggregateConditionReq> getEffectiveAggregateConditions() {
        if (aggregateConditions != null && !aggregateConditions.isEmpty()) {
            return aggregateConditions;
        }
        if (hasSingleAggregateCondition()) {
            AggregateConditionReq condition = new AggregateConditionReq();
            condition.setAttrType(attrType);
            condition.setAttrCode(attrCode);
            condition.setComparator(attrComparator);
            condition.setAttrValue(attrValue);
            return Collections.singletonList(condition);
        }
        if (hasWhereOnlyCondition()) {
            return Collections.singletonList(defaultQuantityCondition());
        }
        return Collections.emptyList();
    }

    public void addAggregateCondition(AggregateConditionReq condition) {
        if (aggregateConditions == null) {
            aggregateConditions = new ArrayList<>();
        }
        aggregateConditions.add(condition);
        if (aggregateConditions.size() == 1) {
            syncSingleAggregateFields(condition);
        }
    }

    public void syncSingleAggregateFields(AggregateConditionReq condition) {
        if (condition == null) {
            return;
        }
        attrType = condition.getAttrType();
        attrCode = condition.getAttrCode();
        attrComparator = condition.getComparator();
        attrValue = condition.getAttrValue();
    }

    public boolean hasWhereOnlyCondition() {
        return attrWhereCondition != null && !attrWhereCondition.isEmpty() && !hasSingleAggregateCondition();
    }

    private boolean hasSingleAggregateCondition() {
        return attrCode != null && !attrCode.isEmpty()
                && attrComparator != null && !attrComparator.isEmpty()
                && attrValue != null && !attrValue.isEmpty();
    }

    private AggregateConditionReq defaultQuantityCondition() {
        AggregateConditionReq condition = new AggregateConditionReq();
        condition.setAttrType(AttrParaType.Sum);
        condition.setAttrCode(PartConstantAttr.Quantity.getCode());
        condition.setComparator(">=");
        condition.setAttrValue("1");
        condition.setDefaulted(true);
        return condition;
    }
}
