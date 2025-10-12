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

    public AlgCPConstraint(Constraint cpConstraint, Literal relaxationVar, String relaxationVarName) {
        this.cpConstraint = cpConstraint;
        this.relaxationVar = relaxationVar;
        this.relaxationVarName = relaxationVarName;

        if (relaxationVar != null) {
            cpConstraint.onlyEnforceIf(relaxationVar.not());
        }
    }

    /**
     * 设置约束执行条件
     * 
     * @param condition 执行条件
     */
    public void onlyEnforceIf(Literal condition) {
        cpConstraint.onlyEnforceIf(condition);
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
}
