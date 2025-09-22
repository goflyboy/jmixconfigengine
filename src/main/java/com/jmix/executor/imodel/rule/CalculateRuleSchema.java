package com.jmix.executor.imodel.rule;

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

    /**
     * 获取左侧编程对象列表
     * 
     * @return 左侧编程对象列表
     */
    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        if (leftExpr != null && leftExpr.getRefProgObjs() != null) {
            return leftExpr.getRefProgObjs();
        }
        return new ArrayList<>();
    }

    /**
     * 获取右侧编程对象列表
     * 
     * @return 右侧编程对象列表
     */
    @Override
    public List<RefProgObjSchema> getToRightProgObjs() {
        // CalculateRule只有左侧表达式，没有右侧表达式
        return new ArrayList<>();
    }
}