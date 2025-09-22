package com.jmix.executor.impl.algmodel;

import com.jmix.executor.imodel.ParaOption;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpSolverSolutionCallback;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数选项变量
 * 用于表示参数选项的选中状态和相关信息
 * 
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParaOptionVar extends Var<ParaOption> {
    /**
     * 参数选项的选中状态（布尔变量）
     */
    private BoolVar isSelectedVar;

    public ParaOptionVar() {
    }

    public ParaOptionVar(ParaOption base) {
        this.base = base;
    }

    public ParaOptionVar(ParaOption base, BoolVar isSelectedVar) {
        this.base = base;
        setIsSelectedVar(isSelectedVar);
    }

    public String getCode() {
        return base != null ? base.getCode() : null;
    }

    public int getCodeId() {
        return base != null ? base.getCodeId() : 0;
    }

    @Override
    public String getVarString(CpSolverSolutionCallback solutionCallback) {
        StringBuilder sb = new StringBuilder();
        sb.append("ParaOption{code=").append(getCode());
        if (getIsSelectedVar() != null) {
            sb.append(", selected=").append(solutionCallback.value(getIsSelectedVar()));

        }
        sb.append("}");
        return sb.toString();
    }
}