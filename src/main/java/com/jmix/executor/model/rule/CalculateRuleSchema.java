package com.jmix.executor.model.rule;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 计算规则Schema
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CalculateRuleSchema extends RuleSchema {
    /**
     * 左表达式
     */
    private ExprSchema leftExpr;

    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        if (leftExpr != null && leftExpr.getRefProgObjs() != null) {
            return leftExpr.getRefProgObjs();
        }
        return new ArrayList<>();
    }

    @Override
    public List<RefProgObjSchema> getToRightProgObjs() {
        // CalculateRule只有左侧表达式，没有右侧表达式
        return new ArrayList<>();
    }
}