package com.jmix.executor.imodel;

import com.jmix.executor.impl.util.ExpressionCalculator;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 优先级约束类
 * 用于定义优化目标的优先级约束
 * 
 * @since 2025-01-XX
 */
@Data
public class PriorityConstraint {
    /**
     * 规则对象
     */
    private Rule rule;

    /**
     * 线性表达式
     */
    // private LinearExpr expr; // 约束求解器里的表达式

    /**
     * 表达式字符串，如：sd1.S*110 + sd2.S*120 + md1.S*13
     */
    private String exprStr;

    /**
     * 表达式模板，如：%d*110 + %d*120 + %d*13，用于计算值（是否一致）
     */
    private String exprTemplate;

    /**
     * 表达式模板字符串，如：sd1.S_%d*110 + sd2.S_%d*120 + md1.S_%d*13，用于展示
     */
    private String exprTemplateStr;

    /**
     * 表达式变量列表
     */
    private List<PartTerm> exprVariables = new ArrayList<>();

    /**
     * 属性代码
     */
    private String attrCode;

    /**
     * 计算并转换为字符串
     * 根据 exprVariables 中 termValue 构建 objectValues，然后格式化表达式并计算结果
     * 
     * @param pc            优先级约束对象
     * @param exprVariables 表达式变量列表
     * @return 格式化的计算结果字符串
     */
    public static String calcToString(PriorityConstraint pc, List<PartTerm> exprVariables) {
        // 根据 exprVariables 中 termValue 构建 objectValues
        Object[] objectValues = new Object[exprVariables.size()];
        for (int i = 0; i < exprVariables.size(); i++) {
            PartTerm term = exprVariables.get(i);
            // termValue 可能为 null，使用 0 作为默认值
            objectValues[i] = term.getTermValue() != null ? term.getTermValue() : 0;
        }

        // 调用公式计算机器计算 calcStr 的值 calcResult，基于 Apache Commons JEXL 3.4.0 实现
        String calcExprStr = String.format(pc.getExprTemplate(), objectValues);
        double calcResult = ExpressionCalculator.calculate(calcExprStr);

        // 打印 printStr = calcResult
        String printStr = String.format(pc.getExprTemplateStr(), objectValues);
        return printStr + " = " + calcResult;
    }

    /**
     * 部件项
     * 表示表达式中的一个项
     */
    @Data
    public static class PartTerm {
        /**
         * 索引
         */
        private int index;

        /**
         * 部件代码
         */
        private String partCode;

        /**
         * 项值，默认是空，可能是 ".S" 的值，也可能是 ".Q" 的值
         */
        private Integer termValue;
    }
}
