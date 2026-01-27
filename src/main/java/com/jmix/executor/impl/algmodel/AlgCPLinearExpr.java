package com.jmix.executor.impl.algmodel;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

import lombok.extern.slf4j.Slf4j;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 线性表达式构建器包装器
 * 用于跟踪构建线性表达式时的所有信息，便于日志记录和调试
 */
@Slf4j
public class AlgCPLinearExpr {
    private LinearExprBuilder builder;
    private List<Map.Entry<String, String>> terms;
    private String name;

    public AlgCPLinearExpr(String name) {
        this.builder = LinearExpr.newBuilder();
        this.terms = new ArrayList<>();
        this.name = name;
        log.info("Linear expression created: {}", name);
    }

    /**
     * 添加变量项到表达式
     *
     * @param var         变量
     * @param coefficient 系数
     */
    public void addTerm(IntVar var, long coefficient) {
        builder.addTerm(var, coefficient);
        String sign = coefficient >= 0 ? "+" : "-";
        String termStr = String.format("%s %d * %s", sign, Math.abs(coefficient), var.getName());
        terms.add(new AbstractMap.SimpleEntry<>(termStr, ""));
        log.debug("Added term to expression {}: {}", name, termStr);
    }

    /**
     * 添加常数项到表达式
     *
     * @param value 常数值
     */
    public void addConstant(long value) {
        builder.add(value);
        String sign = value >= 0 ? "+" : "-";
        String termStr = String.format("%s %d", sign, Math.abs(value));
        terms.add(new AbstractMap.SimpleEntry<>(termStr, ""));
        log.debug("Added constant to expression {}: {}", name, termStr);
    }

    /**
     * 添加线性表达式项到表达式（带系数）
     *
     * @param expr        线性表达式
     * @param coefficient 系数
     */
    public void addExpr(AlgCPLinearExpr expr, long coefficient) {
        builder.addTerm(expr.build(), coefficient);
        String sign = coefficient >= 0 ? "+" : "-";
        String termStr = String.format("%s %d * (%s)", sign, Math.abs(coefficient), expr.toString());
        terms.add(new AbstractMap.SimpleEntry<>(termStr, expr.getName()));
        log.debug("Added expression to {}: {}", name, termStr);
    }

    /**
     * 构建LinearExpr
     *
     * @return 构建的线性表达式
     */
    public LinearExpr build() {
        log.info("Building linear expression: {}", name);
        return builder.build();
    }

    /**
     * 获取表达式名称
     *
     * @return 表达式名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取所有项的列表
     *
     * @return 不可修改的项列表
     */
    public List<String> getTerms() {
        List<String> termStrings = new ArrayList<>();
        for (Map.Entry<String, String> term : terms) {
            termStrings.add(term.getKey());
        }
        return Collections.unmodifiableList(termStrings);
    }

    /**
     * 返回表达式的字符串表示
     *
     * @return 格式化的表达式字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> term : terms) {
            String termStr = term.getKey();
            if (first) {
                // 处理第一个项的符号
                if (termStr.startsWith("+ ")) {
                    sb.append(termStr.substring(2));
                } else if (termStr.startsWith("- ")) {
                    sb.append(termStr);
                } else {
                    sb.append(termStr);
                }
                first = false;
            } else {
                sb.append(" ").append(termStr);
            }
        }

        if (terms.isEmpty()) {
            sb.append("0");
        }
        return sb.toString();
    }

    /**
     * 返回表达式的详细字符串表示
     *
     * @return 格式化的表达式字符串
     */
    public String toDetailString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> term : terms) {
            String termStr = term.getKey();
            String exprName = term.getValue();
            // 打印 term.second（表达式名称）
            if (!exprName.isEmpty()) {
                sb.append("\n");
                sb.append("  ").append(exprName).append(": ");
            }
            if (first) {
                // 处理第一个项的符号
                if (termStr.startsWith("+ ")) {
                    sb.append(termStr.substring(2));
                } else if (termStr.startsWith("- ")) {
                    sb.append(termStr);
                } else {
                    sb.append(termStr);
                }
                first = false;
            } else {
                sb.append(" ").append(termStr);
            }
        }

        if (terms.isEmpty()) {
            sb.append("0");
        }
        return sb.toString();
    }
}
