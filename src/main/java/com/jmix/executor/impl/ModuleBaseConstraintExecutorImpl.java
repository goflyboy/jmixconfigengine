package com.jmix.executor.impl;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.cmodel.SolverResult;
import com.jmix.executor.impl.algmodel.AlgCPModel;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.InferPartCategoryReq;
import com.jmix.executor.model.ParConstraint;
import com.jmix.executor.model.Result;

import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 模块基础约束执行器抽象类
 * 提供模块约束执行的公共功能
 *
 * @since 2025-01-XX
 */
@Slf4j
public abstract class ModuleBaseConstraintExecutorImpl {

    protected final Module module;
    protected final ConstraintConfig config;
    protected final ModuleAlgClassLoader moduleAlgClassLoader;

    public ModuleBaseConstraintExecutorImpl(Module module, ConstraintConfig config,
            ModuleAlgClassLoader moduleAlgClassLoader) {
        this.module = module;
        this.config = config;
        this.moduleAlgClassLoader = moduleAlgClassLoader;
    }

    /**
     * 执行约束处理的主方法，由子类实现
     *
     * @param partCategoryReq 部件分类请求
     * @return 处理结果
     */
    public abstract Result<List<ModuleInst>> doProcess(InferPartCategoryReq partCategoryReq);

    /**
     * 求解约束模型并返回结果（无优先级约束）
     *
     * @param filteredModuleBase 过滤后的模块基础
     * @param partCatagoryReq   部件分类请求
     * @return 求解结果
     */
    protected SolverResult solveWithOutPriorityConstraints(IModule filteredModuleBase,
            InferPartCategoryReq partCatagoryReq) {
        log.info("no Priority-process starting........");
        return invokerSolver(filteredModuleBase, partCatagoryReq, new ArrayList<>(), null);
    }

    /**
     * 验证优先级总体值
     */
    protected Pair<Boolean, String> validPriorityOverallValue(double solverValue, List<ModuleInst> exprValueSolutions) {
        StringBuffer sb = new StringBuffer();
        boolean result = false;

        if (exprValueSolutions == null || exprValueSolutions.isEmpty()) {
            sb.append("exprValueSolutions is empty");
            result = false;
        } else {
            double priorityOverallValue = exprValueSolutions.get(0).getPriorityOverallValue();
            if (Double.compare(priorityOverallValue, solverValue) != 0) {
                sb.append(String.format(
                        "Priority overall value mismatch: solverValue=%.2f, exprValueSolutions.get(0).getPriorityOverallValue()=%.2f",
                        solverValue, priorityOverallValue));
                result = false;
            } else {
                result = true;
            }
        }

        return Pair.of(result, sb.toString());
    }

    /**
     * 获取调整后的目标值
     */
    protected double getAdjustObjectValue(double objectValue, double adjustCoeff) {
        int cResult = Double.compare(objectValue, 0.0);
        if (cResult < 0) {
            if (adjustCoeff >= 0) {
                objectValue = objectValue * (1 - adjustCoeff);
            } else {
                objectValue = objectValue * (1 + Math.abs(adjustCoeff));
            }
        } else if (cResult == 0) {
            objectValue = adjustCoeff;
        } else {
            if (adjustCoeff >= 0) {
                objectValue = objectValue * (1 + adjustCoeff);
            } else {
                objectValue = objectValue * (1 - Math.abs(adjustCoeff));
            }
        }
        return objectValue;
    }

    /**
     * 求解器调用入口
     */
    protected SolverResult invokerSolver(
            IModule filteredModuleBase, InferPartCategoryReq partCatagoryReq,
            List<ParConstraint> partConstraintFromReqs, Double adjustOptimalValue) {
        log.info("invokerSolver starting........");
        
        // 步骤1：对每个优先级规则优化求解最优解
        ConstraintAlgImpl optAlg = createConstraintAlg(module.getId(), module.getCode());
        AlgCPModel optModel = new AlgCPModel();
        optAlg.initModel(optModel, filteredModuleBase, false, new ArrayList<>(), partConstraintFromReqs);
        initModelByReq(partCatagoryReq, optAlg);
        initModelByPriorityConstraints(partConstraintFromReqs, optAlg);
        optAlg.addRelaxObjectFunction();
        boolean hasPriorityRule = optAlg.hasPriorityRule();
        if (hasPriorityRule) {
            PartAlgCPLinearExpr priorityMergedExpr = optAlg.queryMergerPriorityConstraintExpr();
            if (adjustOptimalValue == null) {
                optModel.minimize(priorityMergedExpr);
            } else {
                optModel.addLessOrEqual(priorityMergedExpr, adjustOptimalValue.longValue());
            }
        }

        // 求解最优解
        CpSolver optSolver = new CpSolver();
        optSolver.getParameters().setNumSearchWorkers(1);
        optSolver.getParameters().setEnumerateAllSolutions(true);
        optSolver.getParameters().setMaxTimeInSeconds(10);
        ModuleInstSolutionCallBack optCb = new ModuleInstSolutionCallBack(module, optAlg.getVars(),
                optAlg.getOtherVarMap(), optAlg);

        log.info("solver  models:\n");
        optModel.printModelSummary();
        CpSolverStatus optStatus = optSolver.solve(optModel.getCpModel(), optCb);
        SolverResult sr = optCb.getSolverResult();
        sr.setSolverStatus(optStatus.toString());
        if (optStatus == CpSolverStatus.OPTIMAL || optStatus == CpSolverStatus.FEASIBLE) {
            sr.setObjectiveValue(optSolver.objectiveValue());
            log.info("Optimal value for : {}", optSolver.objectiveValue());
        } else {
            log.error("Failed to solver, status : " + optStatus);
            return sr;
        }
        if (hasPriorityRule && (adjustOptimalValue == null)) {
            Pair<Boolean, String> validResult = validPriorityOverallValue(sr.getObjectiveValue(), sr.getSolutions());
            if (!validResult.getFirst()) {
                String msg = "Failed to valid expr&solver " + validResult.getSecond();
                log.error(msg);
            }
        }
        sortSolutionsByPriority(sr.getSolutions());
        log.info("solutions: \n {}",
                SolutionUtils.toSolutionString(sr.getSolutions()));
        return sr;
    }

    /**
     * 根据优先级对解决方案进行排序
     */
    protected void sortSolutionsByPriority(List<ModuleInst> solutions) {
        solutions.sort((a, b) -> -Double.compare(b.getPriorityOverallValue(), a.getPriorityOverallValue()));
        for (int i = 0; i < solutions.size(); i++) {
            solutions.get(i).setPrioritySortNo(i + 1);
        }
    }

    /**
     * 根据请求初始化约束模型
     */
    protected void initModelByReq(InferPartCategoryReq req, ConstraintAlgImpl alg) {
        if (req.getPreParaInsts() != null) {
            for (ParaInst paraInst : req.getPreParaInsts()) {
                alg.addParaEquality(paraInst.getCode(), paraInst.getValue());
            }
        }
        if (req.getPrePartInsts() != null) {
            for (PartInst partInst : req.getPrePartInsts()) {
                alg.addPartEquality(partInst.getCode(), partInst.getQuantity());
            }
        }
    }

    /**
     * 根据请求初始化优先级约束模型
     */
    protected void initModelByPriorityConstraints(
            List<ParConstraint> partConstraintFromReqs, ConstraintAlgImpl alg) {
        for (ParConstraint partConstraint : partConstraintFromReqs) {
            if (partConstraint.getFilteredCategory() != null) {
                alg.sumFunConstraint(partConstraint.getFilteredCategory().getAllAtomicParts(), partConstraint);
            }
        }
    }

    /**
     * 创建约束算法实例
     */
    protected ConstraintAlgImpl createConstraintAlg(long moduleId, String moduleCode) {
        return moduleAlgClassLoader.newConstraintAlg(moduleCode);
    }
}

