package com.jmix.temp3.core;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

import java.util.HashMap;
import java.util.Map;

/**
 * 表达式计算器
 * 基于 Apache Commons JEXL 3.4.0 实现表达式计算
 * 
 * @since 2025-01-XX
 */
@Slf4j
public final class ExpressionCalculator {

    private static final JexlEngine JEXL = new JexlBuilder().create();

    /**
     * 隐藏构造器
     */
    private ExpressionCalculator() {
    }

    /**
     * 计算表达式
     *
     * @param expression 表达式字符串，如："1*110 + 2*120 + 3*13"
     * @return 计算结果
     */
    public static double calculate(String expression) {
        return calculate(expression, new HashMap<>());
    }

    /**
     * 计算表达式，使用提供的变量映射
     *
     * @param expression 表达式字符串，如："- 100 * (3 * sd1_Q) + 1 * (1 * md1_Q)"
     * @param part       变量映射，包含表达式中使用的变量值
     * @return 计算结果
     */
    public static double calculate(String expression, Map<String, Integer> parts) {
        try {
            JexlExpression expr = JEXL.createExpression(expression);
            JexlContext context = new MapContext();

            // 将变量映射添加到上下文中
            if (parts != null) {
                for (Map.Entry<String, Integer> entry : parts.entrySet()) {
                    context.set(entry.getKey(), entry.getValue());
                }
            }

            Object result = expr.evaluate(context);
            // System.out.println(expr.getSourceText());
            // System.out.println(expr.getParsedText());
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            } else {
                log.warn("Expression result is not a number: {}", result);
                return 0.0;
            }
        } catch (Exception e) {
            log.error("Failed to calculate expression: {} with variables: {}", expression, parts, e);
            return 0.0;
        }
    }

    public static void main(String[] args) {
        // 测试表达式：- 100 * (3 * sd1_Q) + 1 * (1 * md1_Q)
        String expression = "- 100 * (3 * sd1_Q) + 1 * (1 * md1_Q)";

        // 创建变量映射
        Map<String, Integer> parts = new HashMap<>();
        parts.put("sd1_Q", 10); // 设置 sd1_Q 的值为 10
        parts.put("md1_Q", 20); // 设置 md1_Q 的值为 20

        // 计算结果
        double result = calculate(expression, parts);

        // 输出结果
        System.out.println("Expression: " + expression);
        System.out.println("Variables: " + parts);
        System.out.println("Result: " + result);

        // 手动验证：-100 * (3 * 10) + 1 * (1 * 20) = -100 * 30 + 20 = -3000 + 20 = -2980
        System.out.println("Expected: -2980");
    }
}