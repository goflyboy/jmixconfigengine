package com.jmix.executor.impl;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.cmodel.SolverResult;
import com.jmix.executor.impl.algmodel.AlgCPModel;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr;
import com.jmix.executor.model.AlgExecutorException;
import com.jmix.executor.model.AttrFunConstant;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.InferPartCategoryReq;
import com.jmix.executor.model.PartConstraintReq;
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

    protected Module module;
    protected ConstraintConfig config;
    protected ModuleAlgClassLoader moduleAlgClassLoader;

    public void init(Module module, ConstraintConfig config,
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
     * @param partCategoryReq    部件分类请求
     * @return 求解结果
     */
    protected SolverResult solveWithOutPriorityConstraints(IModule filteredModuleBase,
            InferPartCategoryReq partCategoryReq,
            List<PartCategoryInputBase> partCategoryInputs) {
        log.info("no Priority-process starting........");
        return invokerSolver(filteredModuleBase, partCategoryReq, partCategoryInputs, null);
    }

    /**
     * 主求解方法-带优先级约束
     */
    protected SolverResult solveWithPriorityConstraints(
            IModule filteredModuleBase, InferPartCategoryReq partCategoryReq,
            List<PartCategoryInputBase> partCategoryInputs) {
        log.info("Priority-process pconstraint-step1 max/min starting........");
        // 步骤1：对每个优先级规则优化求解最优解 minimize(objecFun)
        SolverResult result = invokerSolver(filteredModuleBase, partCategoryReq, partCategoryInputs, null);

        if (!result.hasSolution()) {
            result.setMessage("Cannot find solution in first step");
            return result;
        }

        // 步骤2：在保持高优先级目标接近最优的前提下，寻找多个解，objecFun <=
        // ajustObjectValue(不断调整ajustObjectValue)
        final int maxExecTimes = 5; // 最大执行次数,
        final int availableSolutionNum = 5; // 可行解的格式
        final int minSolutionThreshold = 3; // 最少解数量阈值
        int execTimes = 0;
        double baseAdjustCoeff = 0.5; // 基础调整系数
        double objectValue = result.getObjectiveValue();
        boolean needIncreased = true; // 是否需要增加objectvalue
        int needIncreasedTimes = 0;
        do {
            execTimes++;
            SolverResult lastResult = result;
            // 根据上一次求解结果动态调整策略
            if (execTimes > 1) {
                if (lastResult.isHasSearchMax()) {
                    // objectValue设置太大，最优解都没有处理，系数需要往下调
                    baseAdjustCoeff *= 0.7; // 降低调整幅度
                    needIncreased = false;
                    needIncreasedTimes = 0;
                    log.info(
                            "Priority-process pconstraint-step2 Iteration {}: objective value too high, reducing adjustment coefficient to {}",
                            execTimes, baseAdjustCoeff);
                } else if (lastResult.getSolutions().size() < minSolutionThreshold) {
                    // 解数量太少，objectValue设置太小，系数需要往上调
                    needIncreasedTimes++;
                    baseAdjustCoeff *= 3 * needIncreasedTimes;
                    needIncreased = true;
                    log.info(
                            "Priority-process pconstraint-step2 Iteration {}: too few solutions found, increasing adjustment coefficient to {}",
                            execTimes, baseAdjustCoeff);
                } else {
                    log.info("xxxx");
                    break;
                }
            }
            // 根据上一次调整方向决定本次调整
            if (!needIncreased) {
                objectValue = getAdjustObjectValue(objectValue, -baseAdjustCoeff);
            } else {
                objectValue = getAdjustObjectValue(objectValue, baseAdjustCoeff);
            }
            log.info("Priority-process pconstraint-step2 Iteration {}: adjusting objective value to {}", execTimes,
                    objectValue);
            result = invokerSolver(filteredModuleBase, partCategoryReq, partCategoryInputs, objectValue);

        } while (execTimes < maxExecTimes
                && result.getSolutions().size() <= availableSolutionNum);
        result.setIterationTimes(execTimes);
        return result;
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
            IModule filteredModuleBase, InferPartCategoryReq partCategoryReq,
            List<PartCategoryInputBase> partCategoryInputs, Double adjustOptimalValue) {
        log.info("invokerSolver starting........");

        // 步骤1：对每个优先级规则优化求解最优解
        ModuleAlgImpl optAlg = createConstraintAlg(module.getId(), module.getCode());
        AlgCPModel optModel = new AlgCPModel();
        optModel.setIsAttachRelax(false);
        optModel.setConfictedRelaxVars(new ArrayList<>());
        optAlg.init(optModel, filteredModuleBase, partCategoryInputs);
        initModelByReq(partCategoryReq, optAlg);
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
        ModuleInstSolutionCallBack optCb = new ModuleInstSolutionCallBack(module, optAlg);

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
    protected void initModelByReq(InferPartCategoryReq req, ModuleAlgImpl alg) {
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
     * 解析并添加部件约束
     *
     * @param filteredCategory  过滤后的部件分类
     * @param partConstraintReq 部件约束请求
     */
    public static PartCategoryInput toPartCategoryInput(PartCategory filteredCategory,
            PartConstraintReq partConstraintReq) {
        // 解析属性
        Pair<DynamicAttribute, String> result = filteredCategory.parseAttribute(
                partConstraintReq.getAttrCode(),
                filteredCategory.getDynAttrSchemas());
        if (!AttrFunConstant.FUN_PREFIX_SUM.equals(result.getSecond())) {
            log.error("attrCode is not sum function");
            throw new AlgExecutorException("attrCode is not sum function");
        }

        // 根据partConstraintReq构造IPartCategoryInput
        PartCategoryInput fromReq = new PartCategoryInput();
        setPartCategoryInputBase(fromReq, partConstraintReq);
        fromReq.setFilteredCategory(filteredCategory);
        return fromReq;
    }

    /**
     * 设置PartCategoryInputBase
     * 
     * @param partCategoryInput
     * @param partConstraintReq
     */
    public static void setPartCategoryInputBase(PartCategoryInputBase partCategoryInput,
            PartConstraintReq partConstraintReq) {
        partCategoryInput.setSumAttrCode(partConstraintReq.getAttrCode());
        partCategoryInput.setAttrType(partConstraintReq.getAttrType());
        partCategoryInput.setComparator(partConstraintReq.getAttrComparator());
        partCategoryInput.setLeftValue(Integer.parseInt(partConstraintReq.getAttrValue()));
        partCategoryInput.setOrgReq(partConstraintReq);
    }

    /**
     * 创建约束算法实例
     */
    protected ModuleAlgImpl createConstraintAlg(long moduleId, String moduleCode) {
        return moduleAlgClassLoader.newConstraintAlg(moduleCode);
    }
}
