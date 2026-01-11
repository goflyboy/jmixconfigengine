package com.jmix.executor.omodel;

import com.jmix.executor.impl.algmodel.AlgCPModel;
import com.jmix.executor.impl.ModuleInstSolutionCallBack;

import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 运行推理参数响应数据类
 * 封装推理参数执行的结果信息
 * 
 * @since 2025-01-XX
 */
@Data
@AllArgsConstructor
public class RunInferParasRsp {
    /**
     * 求解器状态
     */
    private final CpSolverStatus status;

    /**
     * 模块实例解决方案回调
     */
    private final ModuleInstSolutionCallBack solutionCallBack;

    /**
     * 算法CP模型
     */
    private final AlgCPModel algCPModel;

    /**
     * CP求解器
     */
    private final CpSolver solver;
}

