package com.jmix.executor.impl.algmodel;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import com.jmix.executor.southinf.cp.AlgCPLinearArgument;
import com.jmix.executor.southinf.cp.AlgCPLinearExpr;
import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPIntVar;

import lombok.Data;
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
@Data
public class AlgCPLinearExprImpl implements AlgCPLinearExpr {

    private LinearExprBuilder builder;

    private List<Map.Entry<String, String>> terms;

    private String exprName;

    /**
     * 实现 AlgCPLinearArgument.name()
     */
    @Override
    public String name() {
        return exprName;
    }

    /**
     * Lombok @Data generates getExprName(), but callers expect getName().
     * Provide getName() as an alias for compatibility.
     */
    public String getName() {
        return exprName;
    }

    /**
     * Lombok setter for exprName.
     */
    public void setExprName(String exprName) {
        this.exprName = exprName;
    }

    /**
     * Compatibility alias for setExprName, used by PartAlgCPLinearExprImpl.
     */
    public void setName(String exprName) {
        this.exprName = exprName;
    }

    /**
     * 是否空
     *
     * @return
     */
    @Override
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
    public static AlgCPLinearExpr term(AlgCPLinearExpr var, long coefficient) {
        AlgCPLinearExprImpl expr = new AlgCPLinearExprImpl("term_" + var.name() + "_" + coefficient);
        expr.addExpr(var, coefficient);
        return expr;
    }

    /**
     * 创建一个包含单个项的线性表达式
     *
     * @param var         变量
     * @param coefficient 系数
     * @return 创建的AlgCPLinearExpr
     */
    public static AlgCPLinearExpr term(IntVar var, long coefficient) {
        AlgCPLinearExprImpl expr = new AlgCPLinearExprImpl("term_" + var.getName() + "_" + coefficient);
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
        AlgCPLinearExprImpl expr = new AlgCPLinearExprImpl("term_" + var.getName() + "_" + coefficient);
        expr.addTerm(var, coefficient);
        return expr;
    }

    /**
     * 创建一个包含单个项的线性表达式（AlgCPIntVar版本）
     *
     * @param var         变量
     * @param coefficient 系数
     * @return 创建的AlgCPLinearExpr
     */
    public static AlgCPLinearExpr term(AlgCPIntVar var, long coefficient) {
        AlgCPLinearExprImpl expr = new AlgCPLinearExprImpl("term_" + var.name() + "_" + coefficient);
        expr.addTerm(var, coefficient);
        return expr;
    }

    /**
     * 创建一个包含单个项的线性表达式（AlgCPBoolVar版本）
     *
     * @param var         变量
     * @param coefficient 系数
     * @return 创建的AlgCPLinearExpr
     */
    public static AlgCPLinearExpr term(AlgCPBoolVar var, long coefficient) {
        AlgCPLinearExprImpl expr = new AlgCPLinearExprImpl("term_" + var.name() + "_" + coefficient);
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
        AlgCPLinearExprImpl result = new AlgCPLinearExprImpl("sum_" + expressions.length + "_alg_exprs");
        for (AlgCPLinearExpr expr : expressions) {
            result.builder.add((com.google.ortools.sat.LinearExpr) expr.build());
            result.terms.add(new AbstractMap.SimpleEntry<>("+ (" + expr.toString() + ")", expr.name()));
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
        AlgCPLinearExprImpl result = new AlgCPLinearExprImpl("sum_algcp_iterable_exprs");
        int count = 0;
        for (AlgCPLinearExpr expr : expressions) {
            result.builder.add((com.google.ortools.sat.LinearExpr) expr.build());
            result.terms.add(new AbstractMap.SimpleEntry<>("+ (" + expr.toString() + ")", expr.name()));
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
        AlgCPLinearExprImpl expr = new AlgCPLinearExprImpl("constant_" + value);
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
        AlgCPLinearExprImpl result = new AlgCPLinearExprImpl("weighted_sum");
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
        AlgCPLinearExprImpl result = new AlgCPLinearExprImpl("weighted_sum");
        for (int i = 0; i < vars.length; i++) {
            result.addTerm(vars[i], weights[i]);
        }
        return result;
    }

    public AlgCPLinearExprImpl(String name) {
        this.builder = LinearExpr.newBuilder();
        this.terms = new ArrayList<>();
        this.exprName = name;
        log.info("Linear expression created: {}", name);
    }

    /**
     * 无参构造函数，使用默认名称
     */
    public AlgCPLinearExprImpl() {
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
        log.debug("Added term to expression {}: {}", exprName, termStr);
    }

    /**
     * 添加常数项到表达式
     *
     * @param value 常数值
     */
    public AlgCPLinearExprImpl addConstant(long value) {
        builder.add(value);
        String sign = value >= 0 ? "+" : "-";
        String termStr = String.format("%s %d", sign, Math.abs(value));
        terms.add(new AbstractMap.SimpleEntry<>(termStr, ""));
        log.debug("Added constant to expression {}: {}", exprName, termStr);
        return this;
    }

    /**
     * 添加线性表达式项到表达式（带系数）
     *
     * @param expr        线性表达式
     * @param coefficient 系数
     */
    public void addExpr(AlgCPLinearExpr expr, long coefficient) {
        builder.addTerm((LinearExpr) expr.build(), coefficient);
        String sign = coefficient >= 0 ? "+" : "-";
        String termStr = String.format("%s %d * (%s)", sign, Math.abs(coefficient), expr.toString());
        terms.add(new AbstractMap.SimpleEntry<>(termStr, expr.name()));
        log.debug("Added expression to {}: {}", exprName, termStr);
    }

    /**
     * 添加IntVar到表达式
     * 
     * @param var IntVar
     */
    public void add(IntVar var) {
        builder.add(var);
    }

    /**
     * 添加IntVar项到表达式（带系数）
     *
     * @param var         变量
     * @param coefficient 系数
     */
    @Override
    public AlgCPLinearExpr addTerm(AlgCPIntVar var, long coefficient) {
        builder.addTerm((com.google.ortools.sat.LinearExpr) var, coefficient);
        String sign = coefficient >= 0 ? "+" : "-";
        String termStr = String.format("%s %d * %s", sign, Math.abs(coefficient), var.name());
        terms.add(new AbstractMap.SimpleEntry<>(termStr, ""));
        log.debug("Added term to expression {}: {}", exprName, termStr);
        return this;
    }

    /**
     * 添加BoolVar项到表达式（带系数）
     *
     * @param var         变量
     * @param coefficient 系数
     */
    @Override
    public AlgCPLinearExpr addTerm(AlgCPBoolVar var, long coefficient) {
        builder.addTerm((com.google.ortools.sat.LinearExpr) var, coefficient);
        String sign = coefficient >= 0 ? "+" : "-";
        String termStr = String.format("%s %d * %s", sign, Math.abs(coefficient), var.name());
        terms.add(new AbstractMap.SimpleEntry<>(termStr, ""));
        log.debug("Added term to expression {}: {}", exprName, termStr);
        return this;
    }

    /**
     * 添加AlgCPLinearArgument项到表达式（带系数）
     */
    public AlgCPLinearExpr addTerm(AlgCPLinearArgument arg, long coefficient) {
        builder.addTerm((LinearExpr) arg.build(), coefficient);
        String sign = coefficient >= 0 ? "+" : "-";
        String termStr = String.format("%s %d * %s", sign, Math.abs(coefficient), arg.name());
        terms.add(new AbstractMap.SimpleEntry<>(termStr, ""));
        log.debug("Added term to expression {}: {}", exprName, termStr);
        return this;
    }

    /**
     * Add a raw OR-Tools LinearArgument (e.g. from getQty returning IntVar).
     */
    public AlgCPLinearExpr addTerm(LinearArgument arg, long coefficient) {
        builder.addTerm((LinearExpr) arg, coefficient);
        terms.add(new AbstractMap.SimpleEntry<>(String.valueOf(arg), ""));
        log.debug("Added raw term to expression {}: {}", exprName, arg);
        return this;
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
    @Override
    public Object build() {
        log.info("Building linear expression: {}", exprName);
        return builder.build();
    }

    /**
     * 设置表达式名称（流式调用）
     *
     * @param name 表达式名称
     * @return this
     */
    @Override
    public AlgCPLinearExpr name(String name) {
        this.exprName = name;
        return this;
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
