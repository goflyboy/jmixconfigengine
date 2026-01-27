package com.jmix.executor.impl.algmodel;

import com.google.ortools.sat.Constraint;
import com.google.ortools.sat.ConstraintProto;
import com.google.ortools.sat.Literal;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 带松弛变量的约束包装类
 * 用于支持约束冲突调试功能
 *
 * @since 2025-01-27
 */
@Slf4j
@Data
public class AlgCPConstraint {
    /**
     * 原始CP-SAT约束对象
     */
    private Constraint cpConstraint;

    /**
     * 松弛变量，用于约束冲突调试
     *
     * <p>
     * 当松弛变量为false时，约束将被"放松"（不强制执行），
     * 从而帮助识别导致无解的约束组合。这是约束冲突调试的核心机制。
     * </p>
     */
    private Literal relaxationVar;

    /**
     * 松弛变量名称，用于调试和日志记录
     *
     * <p>
     * 松弛变量名称通常包含约束的标识信息，便于在冲突分析时
     * 快速定位问题约束。例如："relax_constraint_rule_001"。
     * </p>
     */
    private String relaxationVarName = "";

    /**
     * 约束名称，如：addGreaterOrEqual
     */
    private String name = "";

    /**
     * 左表达式的str
     */
    private String left = "";

    /**
     * 操作符
     */
    private String operator = "";

    /**
     * 右表达式的str
     */
    private String right = "";

    /**
     * 条件信息
     */
    private String ifMemo = "";

    /**
     * 左表达式名称
     */
    private String leftName = "";

    /**
     * 右表达式名称
     */
    private String rightName = "";

    public AlgCPConstraint(Constraint cpConstraint, Literal relaxationVar, String relaxationVarName) {
        this.cpConstraint = cpConstraint;
        this.relaxationVar = relaxationVar;
        this.relaxationVarName = relaxationVarName;

        if (relaxationVar != null && cpConstraint != null) {
            cpConstraint.onlyEnforceIf(relaxationVar.not());
        }
        log.info("Constraint created: {} with relaxation: {}", name, relaxationVarName);
    }

    /**
     * 设置约束执行条件
     *
     * @param condition 执行条件
     */
    public void onlyEnforceIf(Literal condition) {
        cpConstraint.onlyEnforceIf(condition);
        ifMemo += " if " + toNameString(condition);
        log.info("relax:{} -----AlgCPConstraint:onlyEnforceIf", relaxationVarName);
    }

    /**
     * 不支持的方法 - 抛出异常
     * 
     * @param builder 构建器
     */
    public AlgCPConstraint(ConstraintProto.Builder builder) {
        throw new UnsupportedOperationException("AlgCPConstraint does not support ConstraintProto.Builder constructor");
    }

    /**
     * 不支持的方法 - 抛出异常
     * 
     * @param lits 字面量数组
     */
    public void onlyEnforceIf(Literal[] lits) {
        throw new UnsupportedOperationException("AlgCPConstraint does not support onlyEnforceIf with Literal array");
    }

    /**
     * 获取约束索引
     * 
     * @return 约束索引
     */
    public int getIndex() {
        return cpConstraint.getIndex();
    }

    /**
     * 不支持的方法 - 抛出异常
     *
     * @return 构建器
     */
    public ConstraintProto.Builder getBuilder() {
        throw new UnsupportedOperationException("AlgCPConstraint does not support getBuilder method");
    }

    /**
     * 将文字转换为名称字符串
     *
     * @param lit 文字对象
     * @return 名称字符串
     */
    private String toNameString(Literal lit) {
        if (lit instanceof com.google.ortools.sat.BoolVar) {
            return ((com.google.ortools.sat.BoolVar) lit).getName();
        }
        if (lit instanceof com.google.ortools.sat.IntVar) {
            return ((com.google.ortools.sat.IntVar) lit).getName();
        }
        if (lit instanceof com.google.ortools.sat.NotBoolVar) {
            String str = lit.toString();
            // str = not(sd1.S(0..1)) --> extract "sd1.S"
            // Parse the string: "not(varName(range))" -> "varName"
            if (str.startsWith("not(") && str.endsWith(")")) {
                String inner = str.substring(4, str.length() - 1); // Remove "not(" and ")"
                int parenIndex = inner.indexOf('(');
                if (parenIndex > 0) {
                    return "!" + inner.substring(0, parenIndex); // Extract variable name before "("
                }
            }
        }
        // Handle other unsupported Literal types
        throw new UnsupportedOperationException(
                "Unsupported Literal type: " + lit.getClass().getSimpleName()
                        + ". Only BoolVar, IntVar, and NotBoolVar are supported. Literal: " + lit);
    }

    /**
     * 返回约束的字符串表示
     *
     * @return 格式化的约束字符串
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(left).append(" ").append(operator).append(" ").append(right).append(ifMemo).append(" (").append(name)
                .append(")");
        if (!leftName.isEmpty()) {
            sb.append(" L:").append(leftName);
        }
        if (!rightName.isEmpty()) {
            sb.append(" R:").append(rightName);
        }
        return sb.toString();
    }
}
