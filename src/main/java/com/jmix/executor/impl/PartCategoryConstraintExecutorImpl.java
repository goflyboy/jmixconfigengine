package com.jmix.executor.impl;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.PriorityRuleSchema;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.cmodel.SolverResult;
import com.jmix.executor.impl.algmodel.*;
import com.jmix.executor.model.*;

import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 部件分类约束执行器实现类
 * 专门处理部件分类相关的约束求解
 *
 * @since 2025-01-XX
 */
@Slf4j
public class PartCategoryConstraintExecutorImpl {
    private final Module module;
    private final ConstraintConfig config;
    private final ModuleAlgClassLoader moduleAlgClassLoader;

    public PartCategoryConstraintExecutorImpl(Module module, ConstraintConfig config, ModuleAlgClassLoader moduleAlgClassLoader) {
        this.module = module;
        this.config = config;
        this.moduleAlgClassLoader = moduleAlgClassLoader;
    }

    /**
     * 处理部件分类约束
     *
     * @param partCatagoryReq 部件分类请求
     * @return 推理结果
     */
    public Result<List<ModuleInst>> processPartCategory(InferPartCategoryReq partCatagoryReq) {
        try {
            // 查找原始部件分类
            PartCategory originalCategory = module.findPartCategory(partCatagoryReq.getPartCatagoryCode());
            if (originalCategory == null) {
                log.error("PartCategory not found: {}", partCatagoryReq.getPartCatagoryCode());
                return Result.failed("PartCategory not found: " + partCatagoryReq.getPartCatagoryCode());
            }
            log.info("Priority-orginal aparts: {}", originalCategory.getAllAtomicPartShortString());
            // 遍历所有部件约束请求，逐步过滤
            PartCategory filteredCategory = originalCategory;
            List<ParConstraint> partConstraintFromReqs = new ArrayList<>();
            for (PartConstraintReq partConstraintReq : partCatagoryReq.getPartConstraintReqs()) {
                // 一条partConstraintReq会被拆分两条规则，一条是过滤规则（根据where条件过滤），一条是约束规则（根据attrCode,
                // attrComparator, attrValue构建约束表达式）
                // 查询满足条件的PartCategory
                filteredCategory = filteredCategory.query(partConstraintReq);
                String msg = String.format("Priority-filtered aparts: {%s} by {%s}",
                        filteredCategory.getAllAtomicPartShortString(),
                        partConstraintReq.toShortString());
                log.info(msg);
                if (filteredCategory.getAllAtomicPartShortString().isEmpty()) {
                    return Result.noSolution(msg);
                }
                resolveAddPartConstraint(filteredCategory, partConstraintReq, partConstraintFromReqs);
            }

            // 检查是否有优先级规则，如果有则使用分级求解
            if (filteredCategory.hasPriorityRule()) {
                PartCategory mergedFilteredCategory = buildMergedFilteredCategory(originalCategory,
                        partConstraintFromReqs);
                return solveWithPriorityConstraints(mergedFilteredCategory,
                        partCatagoryReq, partConstraintFromReqs);
            }
            return solveWithOutPriorityConstraints(filteredCategory, partCatagoryReq);

        } catch (Exception e) {
            log.error("Failed to process part category constraint", e);
            return Result.failed("Failed to process part category constraint: " + e.getMessage());
        }
    }

    /**
     * 求解约束模型并返回结果
     *
     * @param filteredCategory 过滤后的部件分类
     * @param partCatagoryReq  部件分类请求
     * @return 求解结果
     */
    private Result<List<ModuleInst>> solveWithOutPriorityConstraints(PartCategory filteredCategory,
                                                                     InferPartCategoryReq partCatagoryReq) {
        log.info("no Priority-process starting........");
        SolverResult result = invokerSolver(filteredCategory, partCatagoryReq, new ArrayList<>(), null);
        return Result.success(result.getSolutions());
    }

    private Pair<Boolean, String> validPriorityOverallValue(double solverValue, List<ModuleInst> exprValueSolutions) {
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

    private Result<List<ModuleInst>> solveWithPriorityConstraints(
            PartCategory filteredCategory, InferPartCategoryReq partCatagoryReq, List<ParConstraint> partConstraintFromReqs) {
        log.info(" Priority-process pconstraint-step1 max/min starting........");
        // 步骤1：对每个优先级规则优化求解最优解
        SolverResult firstResult = invokerSolver(filteredCategory, partCatagoryReq, partConstraintFromReqs, null);
        // 步骤2：在保持高优先级目标接近最优的前提下，寻找多个解
        log.info(" Priority-process pconstraint-step2 to find multiple solutions starting........");
        SolverResult secondResult = invokerSolver(filteredCategory, partCatagoryReq, partConstraintFromReqs, firstResult.getObjectiveValue());
        return Result.success(secondResult.getSolutions());
    }

    private SolverResult invokerSolver(
            PartCategory filteredCategory, InferPartCategoryReq partCatagoryReq, List<ParConstraint> partConstraintFromReqs, Double adjustOptimalValue) {

        log.info(" Priority-process pconstraint-step1 max/min starting........");
        // 步骤1：对每个优先级规则优化求解最优解
        ConstraintAlgImpl optAlg = createConstraintAlg(module.getId(), module.getCode());
        AlgCPModel optModel = new AlgCPModel();
        optAlg.initModel(optModel, filteredCategory, false, new ArrayList<>(), partConstraintFromReqs);
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
        SolverResult sr = new SolverResult();
        ModuleInstSolutionCallBack optCb = new ModuleInstSolutionCallBack(module, optAlg.getVars(),
                optAlg.getOtherVarMap(), optAlg);

        log.info(" Priority-process pconstraint-step1 solver  models:\n");
        optModel.printModelSummary();
        CpSolverStatus optStatus = optSolver.solve(optModel.getCpModel(), optCb);
        sr.setSolutions(optCb.getAllSolutions());
        sr.setSolverStatus(optStatus.toString());
        if (optStatus == CpSolverStatus.OPTIMAL || optStatus == CpSolverStatus.FEASIBLE) {
            sr.setObjectiveValue(optSolver.objectiveValue());
            log.info("Priority-process pconstraint-step1 max/min Optimal value for : {}", optSolver.objectiveValue());
        } else {
            log.error("Priority-process pconstraint-step1 solver, status : " + optStatus);
            return sr;
        }
        if (hasPriorityRule && (adjustOptimalValue == null)) {
            Pair<Boolean, String> validResult = validPriorityOverallValue(sr.getObjectiveValue(), optCb.getAllSolutions());
            if (!validResult.getFirst()) {
                String msg = "Priority-process pconstraint-step1 max/min valid expr&solver " + validResult.getSecond();
                log.error(msg);
                throw new AlgExecutorException(msg);
            }
            sortSolutionsByPriority(sr.getSolutions());
        }
        log.info("Priority-process pconstraint-step1 max/min finished-Optimal solution: \n {}",
                SolutionUtils.toSolutionString(sr.getSolutions()));
        return sr;
    }

    /**
     * 步骤2：在保持高优先级目标接近最优的前提下，寻找多个解
     *
     * @param module                 模块对象
     * @param filteredCategory       过滤后的部件分类
     * @param isAttachRelax          是否附加松弛变量
     * @param confictedRelaxs        冲突松弛变量列表
     * @param partCatagoryReq        部件分类请求
     * @param partConstraintFromReqs 部件约束列表
     * @param optimalValue           最优值列表
     * @return 求解结果
     */
    private SolverResult solveMultipleSolutionsWithPriorityConstraints(Module module,
                                                                       PartCategory filteredCategory, boolean isAttachRelax, List<RelaxVar> confictedRelaxs,
                                                                       InferPartCategoryReq partCatagoryReq, List<ParConstraint> partConstraintFromReqs,
                                                                       Double optimalValue) {
        SolverResult solverResult = new SolverResult();

        log.info(" Priority-process pconstraint-step2 greater/less starting........");
        // 创建新的算法实例和模型
        ConstraintAlgImpl multiAlg = createConstraintAlg(module.getId(), module.getCode());
        AlgCPModel multiModel = new AlgCPModel();
        multiAlg.initModel(multiModel, filteredCategory, isAttachRelax, confictedRelaxs, partConstraintFromReqs);
        initModelByReq(partCatagoryReq, multiAlg);
        initModelByPriorityConstraints(partConstraintFromReqs, multiAlg);
        multiAlg.addRelaxObjectFunction();

        // 对每个最优值添加约束：保持高优先级目标在最优值的30%范围内
        double threshold = 0.3;
        double maxValue = 1000;//optimalValue
        PartAlgCPLinearExpr mergedExpr = multiAlg.queryMergerPriorityConstraintExpr();
        multiModel.addLessOrEqual(mergedExpr, (long) maxValue);
        log.info(
                " Priority-process pconstraint-step2 greater/less Added Priority adjusted constraint: <= {}",
                (long) maxValue);

        log.info(" Priority-process pconstraint-step2 greater/less finished........");
        log.info(" Priority-process pconstraint-step3 solver starting........");
        // 求解多个解
        CpSolver multiSolver = new CpSolver();
        multiSolver.getParameters().setEnumerateAllSolutions(true);
        multiSolver.getParameters().setNumSearchWorkers(1);
        log.info(" Priority-process  pconstraint-step3 solver  models:\n");
        multiModel.printModelSummary();
        // 获取最大解数量限制
        int maxSolutionNum = partCatagoryReq.getMaxSolutionNum() > 0 ? partCatagoryReq.getMaxSolutionNum() : 10;
        ModuleInstSolutionCallBack multiCb = new ModuleInstSolutionCallBack(module, multiAlg.getVars(),
                multiAlg.getOtherVarMap(), multiAlg, maxSolutionNum);
        CpSolverStatus multiStatus = multiSolver.solve(multiModel.getCpModel(), multiCb);

        solverResult.setSolverStatus(multiStatus.toString());

        if (multiStatus == CpSolverStatus.OPTIMAL || multiStatus == CpSolverStatus.FEASIBLE) {
            solverResult.getSolutions().addAll(multiCb.getAllSolutions());
            log.info(" Priority-process pconstraint-step3 solver Found {} solutions ",
                    solverResult.getSolutions().size());
        } else {
            log.warn(
                    " Priority-process pconstraint-step3 solver Failed to find multiple solutions, status: {}",
                    multiStatus);
        }

        return solverResult;
    }

    private void sortSolutionsByPriority(List<ModuleInst> solutions) {
        // 根据 priorityOverallValue 进行排序（降序，值越大优先级越高）
        solutions.sort((a, b) -> Double.compare(b.getPriorityOverallValue(), a.getPriorityOverallValue()));

        // 对 prioritySortNo 进行赋值
        for (int i = 0; i < solutions.size(); i++) {
            solutions.get(i).setPrioritySortNo(i + 1);
        }
    }

    /**
     * 根据请求初始化约束模型
     *
     * @param req 部件分类请求
     * @param alg 约束算法实现
     */
    private void initModelByReq(InferPartCategoryReq req, ConstraintAlgImpl alg) {
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
     * 将部件列表添加到合并的原子部件Map中
     *
     * @param mergedAtomicParts 合并的原子部件Map
     * @param parts             部件列表
     */
    private void addPartsToMap(Map<String, Part> mergedAtomicParts, List<Part> parts) {
        for (Part part : parts) {
            mergedAtomicParts.put(part.getCode(), part);
        }
    }

    /**
     * 构建合并后的过滤分类
     * 仅支持两层结果，不构建结构
     * 使用扣除法原则：
     * - 含有父分类，则求并集
     * - 不含有父分类，补充没有在里面的子分类的部件
     *
     * @param originalCategory       原始部件分类
     * @param partConstraintFromReqs 部件约束列表
     * @return 合并后的过滤分类
     */
    private PartCategory buildMergedFilteredCategory(PartCategory originalCategory,
                                                     List<ParConstraint> partConstraintFromReqs) {
        // 实现案例
        // --drive PC
        // ----sd PC
        // -------sd1 part
        // -------sd2 part
        // -------sd3 part
        // ----md PC
        // -------md1 part
        // -------md2 part
        // -------md3 part

        // 含有父，则求并集
        // input1: req1:{sd->sd1,sd2} req2:{drive-> md1} --> mReq={sd1,sd2,md1}
        // input1: req1:{sd->sd1,sd2} req2:{drive-> sd1} --> mReq={sd1,sd2}
        // 不还原其层次结构

        // 不含有父，补充没有在里面的子分类的部件
        // input1: req1:{sd->sd1,sd2} req2:{md->md1} --> mReq={sd1,sd2,md1} // 各个孩子都有
        // *input2: req1:{sd->sd1,sd2} req2:{sd->sd1} --> mReq={sd1,sd2,md1,md2,md3} 补充

        Map<String, Part> mergedAtomicParts = new HashMap<>();
        Set<String> allPartCategoryCodes = new HashSet<>();

        // 收集所有约束请求的分类代码和原子部件
        for (ParConstraint partConstraint : partConstraintFromReqs) {
            String categoryCode = partConstraint.getOrgReq().getPartCatagoryCode();
            allPartCategoryCodes.add(categoryCode);
            // 合并原子部件
            List<Part> atomicParts = partConstraint.getFilteredCategory().getAllAtomicParts();
            addPartsToMap(mergedAtomicParts, atomicParts);
        }

        // 判断父分类是否包含在allPartCategoryCodes中
        if (!allPartCategoryCodes.contains(originalCategory.getCode())) {
            // 要把没有包含的子分类的部件补充上
            List<PartCategory> partCategoryList = originalCategory.getPartCategorys();
            for (PartCategory subPartCategory : partCategoryList) {
                if (!allPartCategoryCodes.contains(subPartCategory.getCode())) {
                    // 补充未包含的子分类的所有原子部件
                    List<Part> subAtomicParts = subPartCategory.getAllAtomicParts();
                    addPartsToMap(mergedAtomicParts, subAtomicParts);
                }
            }
        }
        // 克隆原始分类并添加合并的部件
        PartCategory resultCategory = originalCategory.clone();
        resultCategory.addAtomicPartsWithoutStructure(new ArrayList<>(mergedAtomicParts.values()));
        return resultCategory;
    }

    /**
     * 根据请求初始化约束模型
     *
     * @param partConstraintFromReqs 部件约束列表
     * @param alg                    约束算法实现
     */
    private void initModelByPriorityConstraints(
            List<ParConstraint> partConstraintFromReqs, ConstraintAlgImpl alg) {
        for (ParConstraint partConstraint : partConstraintFromReqs) {
            alg.sumFunConstraint(partConstraint.getFilteredCategory().getAllAtomicParts(), partConstraint);
        }
    }

    /**
     * 解析并添加部件约束
     *
     * @param filteredCategory       过滤后的部件分类
     * @param partConstraintReq      部件约束请求
     * @param partConstraintFromReqs 部件约束列表
     */
    private void resolveAddPartConstraint(PartCategory filteredCategory, PartConstraintReq partConstraintReq,
                                          List<ParConstraint> partConstraintFromReqs) {
        // 解析属性
        Pair<DynamicAttribute, String> result = filteredCategory.parseAttribute(
                partConstraintReq.getAttrCode(),
                filteredCategory.getDynAttrSchemas());
        if (!AttrFunConstant.FUN_PREFIX_SUM.equals(result.getSecond())) {
            return;
        }

        // 根据partConstraintReq构造ParConstraint
        ParConstraint fromReq = new ParConstraint();
        fromReq.setSumAttrCode(result.getFirst().getCode());
        fromReq.setComparator(partConstraintReq.getAttrComparator());
        fromReq.setLeftValue(Integer.parseInt(partConstraintReq.getAttrValue()));
        fromReq.setOrgReq(partConstraintReq);
        fromReq.setFilteredCategory(filteredCategory);
        partConstraintFromReqs.add(fromReq);
    }

    /**
     * 创建约束算法实例
     *
     * @param moduleId   模块ID
     * @param moduleCode 模块代码
     * @return 约束算法实例
     */
    private ConstraintAlgImpl createConstraintAlg(long moduleId, String moduleCode) {
        return moduleAlgClassLoader.newConstraintAlg(moduleCode);
    }
}
