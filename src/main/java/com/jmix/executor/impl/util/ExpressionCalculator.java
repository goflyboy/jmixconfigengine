package com.jmix.executor.impl.util;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

/**
 * 表达式计算器
 * 基于 Apache Commons JEXL 3.4.0 实现表达式计算
 * 
 * @since 2025-01-XX
 */
@Slf4j
public class ExpressionCalculator {

    private static final org.apache.commons.jexl3.JexlEngine jexl = new JexlBuilder().create();

    /**
     * 计算表达式
     * 
     * @param expression 表达式字符串，如："1*110 + 2*120 + 3*13"
     * @return 计算结果
     */
    public static double calculate(String expression) {
        try {
            JexlExpression expr = jexl.createExpression(expression);
            JexlContext context = new MapContext();
            Object result = expr.evaluate(context);

            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            } else {
                log.warn("Expression result is not a number: {}", result);
                return 0.0;
            }
        } catch (Exception e) {
            log.error("Failed to calculate expression: {}", expression, e);
            return 0.0;
        }
    }
}
