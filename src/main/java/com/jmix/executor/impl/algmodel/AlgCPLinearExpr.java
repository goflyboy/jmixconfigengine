package com.jmix.executor.impl.algmodel;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

import lombok.extern.slf4j.Slf4j;

import java.util.AbstractMap;
import java.util.ArrayList;
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

    /**
     * 是否空
     *
     * @return
     */
    public boolean isEmpty() {
        return terms.isEmpty();
    }

    /**
     * 创建一个包含单个项的线性表达式
     *
     * @param var         变量
     * @param coefficient 系数
     * @return 创建的AlgCPLinearExpr
     */
    public static AlgCPLinearExpr term(IntVar var, long coefficient) {
        AlgCPLinearExpr expr = new AlgCPLinearExpr("term_" + var.getName() + "_" + coefficient);
        expr.addTerm(var, coefficient);
        return expr;
    }

    /**
     * 创建一个包含单个项的线性表达式
     *
     * @param var         变量
     * @param coefficient 系数
     * @return 创建的AlgCPLinearExpr
     */
    public static AlgCPLinearExpr term(BoolVar var, long coefficient) {
        AlgCPLinearExpr expr = new AlgCPLinearExpr("term_" + var.getName() + "_" + coefficient);
        expr.addTerm(var, coefficient);
        return expr;
    }

    /**
     * 对多个AlgCPLinearExpr求和
     *
     * @param expressions 要求和的表达式数组
     * @return 求和后的AlgCPLinearExpr
     */
    public static AlgCPLinearExpr sum(AlgCPLinearExpr... expressions) {
        AlgCPLinearExpr result = new AlgCPLinearExpr("sum_" + expressions.length + "_alg_exprs");
        for (AlgCPLinearExpr expr : expressions) {
            result.builder.add(expr.build());
            result.terms.add(new AbstractMap.SimpleEntry<>("+ (" + expr.toString() + ")", expr.getName()));
        }
        log.debug("Created sum expression with {} AlgCPLinearExpr terms", expressions.length);
        return result;
    }

    /**
     * 对多个AlgCPLinearExpr求和
     *
     * @param expressions 要求和的表达式集合
     * @return 求和后的AlgCPLinearExpr
     */
    public static AlgCPLinearExpr sumAlgCP(Iterable<AlgCPLinearExpr> expressions) {
        AlgCPLinearExpr result = new AlgCPLinearExpr("sum_algcp_iterable_exprs");
        int count = 0;
        for (AlgCPLinearExpr expr : expressions) {
            result.builder.add(expr.build());
            result.terms.add(new AbstractMap.SimpleEntry<>("+ (" + expr.toString() + ")", expr.getName()));
            count++;
        }
        log.debug("Created sum expression with {} AlgCPLinearExpr terms from iterable", count);
        return result;
    }

    /**
     * 创建常量表达式
     *
     * @param value 常量值
     * @return 常量AlgCPLinearExpr
     */
    public static AlgCPLinearExpr constant(long value) {
        AlgCPLinearExpr expr = new AlgCPLinearExpr("constant_" + value);
        expr.addConstant(value);
        return expr;
    }

    /**
     * 创建缩放表达式
     *
     * @param var   变量
     * @param scale 缩放因子
     * @return 缩放后的AlgCPLinearExpr
     */
    public static AlgCPLinearExpr scaled(IntVar var, long scale) {
        return term(var, scale);
    }

    /**
     * 创建缩放表达式
     *
     * @param var   变量
     * @param scale 缩放因子
     * @return 缩放后的AlgCPLinearExpr
     */
    public static AlgCPLinearExpr scaled(BoolVar var, long scale) {
        return term(var, scale);
    }

    /**
     * 创建带权重的求和表达式
     *
     * @param vars    变量数组
     * @param weights 权重数组
     * @return 加权求和的AlgCPLinearExpr
     */
    public static AlgCPLinearExpr weightedSum(IntVar[] vars, long[] weights) {
        if (vars.length != weights.length) {
            throw new IllegalArgumentException("Variables and weights arrays must have the same length");
        }
        AlgCPLinearExpr result = new AlgCPLinearExpr("weighted_sum");
        for (int i = 0; i < vars.length; i++) {
            result.addTerm(vars[i], weights[i]);
        }
        return result;
    }

    /**
     * 创建带权重的求和表达式
     *
     * @param vars    变量数组
     * @param weights 权重数组
     * @return 加权求和的AlgCPLinearExpr
     */
    public static AlgCPLinearExpr weightedSum(BoolVar[] vars, long[] weights) {
        if (vars.length != weights.length) {
            throw new IllegalArgumentException("Variables and weights arrays must have the same length");
        }
        AlgCPLinearExpr result = new AlgCPLinearExpr("weighted_sum");
        for (int i = 0; i < vars.length; i++) {
            result.addTerm(vars[i], weights[i]);
        }
        return result;
    }

    public AlgCPLinearExpr(String name) {
        this.builder = LinearExpr.newBuilder();
        this.terms = new ArrayList<>();
        this.name = name;
        log.info("Linear expression created: {}", name);
    }

    /**
     * 无参构造函数，使用默认名称
     */
    public AlgCPLinearExpr() {
        this("unnamed_expr");
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
     * 批量添加多个项
     *
     * @param vars         变量数组
     * @param coefficients 系数数组
     */
    public void addTerms(IntVar[] vars, long[] coefficients) {
        if (vars.length != coefficients.length) {
            throw new IllegalArgumentException("Variables and coefficients arrays must have the same length");
        }
        for (int i = 0; i < vars.length; i++) {
            addTerm(vars[i], coefficients[i]);
        }
    }

    /**
     * 批量添加多个项
     *
     * @param vars         变量数组
     * @param coefficients 系数数组
     */
    public void addTerms(BoolVar[] vars, long[] coefficients) {
        if (vars.length != coefficients.length) {
            throw new IllegalArgumentException("Variables and coefficients arrays must have the same length");
        }
        for (int i = 0; i < vars.length; i++) {
            addTerm(vars[i], coefficients[i]);
        }
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
