package com.jmix.executor.impl;

import com.jmix.executor.bmodel.logic.PriorityRuleSchema;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr;
import com.jmix.executor.impl.util.ExpressionCalculator;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 优先级约束类
 * 用于定义优化目标的优先级约束
 * 
 * @since 2025-01-XX
 */
@Data
@Slf4j
public class PriorityConstraint {
    /**
     * 规则对象
     */
    private Rule rule;

    /**
     * 线性表达式
     */
    private PartAlgCPLinearExpr expr;

    // 以下字段保留为注释，仅通过 getter 暴露（数据由 expr 提供）
    /*
     * private String exprStr;
     * private String exprTemplate;
     * private String exprTemplateStr;
     * private List<PartTerm> exprVariables = new ArrayList<>();
     */
    /**
     * 表达式模板，如：%d*110 + %d*120 + %d*13，用于计算值（是否一致）
     * 
     * @return
     */
    public String getExprStr() {
        if (expr == null) {
            return "";
        }
        return expr.getExprStr();
    }

    public String getExprTemplate() {
        if (expr == null) {
            return "";
        }
        return expr.getExprTemplate();
    }

    /**
     * 表达式模板字符串，如：sd1.S_%d*110 + sd2.S_%d*120 + md1.S_%d*13，用于展示
     * 
     * @return
     */
    public String getExprTemplateStr() {
        if (expr == null) {
            return "";
        }
        return expr.getExprTemplateStr();
    }

    /**
     * 表达式变量列表
     * 
     * @return
     */
    public List<PartTerm> getExprVariables() {
        if (expr == null) {
            return new ArrayList<>();
        }
        // convert from PriorityConstraint.PartTerm (same type) if possible
        return expr.getPartTerms();
    }

    /**
     * 计算表达式结果
     * 根据 exprVariables 中的值构建 objectValues，然后格式化表达式并计算结果
     * 
     * @param pc            优先级约束对象
     * @param exprVariables 表达式变量值列表
     * @return 计算结果
     */
    public static double calcToString(PartAlgCPLinearExpr pc, List<Integer> exprVariables) {

        // 调用公式计算机器计算 calcStr 的值 calcResult，基于 Apache Commons JEXL 3.4.0 实现
        String calcExprStr = instanceExprTemplate(pc.getExprTemplate(), exprVariables);
        String calcExprTemplateStr = instanceExprTemplateStr(pc.getExprTemplateStr(), exprVariables);
        double calcResult = ExpressionCalculator.calculate(calcExprStr);
        log.info("Priority:: {} = {}",
                calcExprTemplateStr,
                calcResult);
        return calcResult;
    }

    /**
     * 实例化表达式模板字符串
     * 根据 exprVariables 中的值替换模板字符串中的占位符
     * 
     * @param exprTemplateStr 表达式模板字符串，如：sd1.S_%d*110 + sd2.S_%d*120 + md1.S_%d*13
     * @param exprVariables   表达式变量值列表
     * @return 实例化后的表达式字符串
     */
    public static String instanceExprTemplateStr(String exprTemplateStr, List<Integer> exprVariables) {
        Object[] objectValues = buildObjectValues(exprVariables);
        return String.format(exprTemplateStr, objectValues);
    }

    /**
     * 实例化表达式模板
     * 根据 exprVariables 中的值替换模板中的占位符
     * 
     * @param exprTemplate  表达式模板，如：%d*110 + %d*120 + %d*13
     * @param exprVariables 表达式变量值列表
     * @return 实例化后的表达式字符串
     */
    public static String instanceExprTemplate(String exprTemplate, List<Integer> exprVariables) {
        Object[] objectValues = buildObjectValues(exprVariables);
        return String.format(exprTemplate, objectValues);
    }

    /**
     * 获取优先级策略
     * 
     * @return 优先级策略，如果无法获取则返回null
     */
    public PriorityStrategy getPriorityStrategy() {
        if (rule == null || rule.getRawCode() == null) {
            return null;
        }
        if (rule.getRawCode() instanceof PriorityRuleSchema) {
            return ((PriorityRuleSchema) rule.getRawCode()).getPriorityStrategy();
        }
        return null;
    }

    /**
     * 根据 exprVariables 构建 objectValues 数组
     * 
     * @param exprVariables 表达式变量值列表
     * @return objectValues 数组
     */
    private static Object[] buildObjectValues(List<Integer> exprVariables) {
        Object[] objectValues = new Object[exprVariables.size()];
        for (int i = 0; i < exprVariables.size(); i++) {
            Integer value = exprVariables.get(i);
            // value 可能为 null，使用 0 作为默认值
            objectValues[i] = value != null ? value : 0;
        }
        return objectValues;
    }

    /**
     * 部件项
     * 表示表达式中的一个项
     */
    @Data
    public static class PartTerm {
        /**
         * 索引，总的，按分类平铺（压扁）
         */
        private int index;

        /**
         * 当前部件所属的部件分类代码，如果是产品级，则为空
         */
        private String partCategoryCode;

        /**
         * 实例ID
         */
        private int instId;

        /**
         * 部件代码
         */
        private String partCode;

        /**
         * 项值，默认是空，可能是 ".S" 的值，也可能是 ".Q" 的值
         */
        private String termValue;
    }
}
