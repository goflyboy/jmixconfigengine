package com.jmix.executor.impl.algmodel;

import com.google.ortools.sat.Constraint;
import com.google.ortools.sat.ConstraintProto;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

/**
 * 带松弛变量的约束包装类
 * 用于支持约束冲突调试功能
 * 
 * @since 2025-01-27
 */
@Slf4j
public class AlgCPConstraint {
    private Constraint cpConstraint = null;
    private Literal relaxationVar = null;
    private String relaxationVarName = null;

    /**
     * 构造函数
     * 
     * @param cpConstraint      原始约束
     * @param relaxationVar     松弛变量
     * @param relaxationVarName 松弛变量名称
     */
    public AlgCPConstraint(Constraint cpConstraint, Literal relaxationVar, String relaxationVarName) {
        this.cpConstraint = cpConstraint;
        this.relaxationVar = relaxationVar;
        this.relaxationVarName = relaxationVarName;

        if (relaxationVar != null) {
            cpConstraint.onlyEnforceIf(relaxationVar);
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

    /**
     * 获取原始约束
     * 
     * @return 原始约束
     */
    public Constraint getCpConstraint() {
        return cpConstraint;
    }

    /**
     * 获取松弛变量
     * 
     * @return 松弛变量
     */
    public Literal getRelaxationVar() {
        return relaxationVar;
    }

    /**
     * 获取松弛变量名称
     * 
     * @return 松弛变量名称
     */
    public String getRelaxationVarName() {
        return relaxationVarName;
    }
}
