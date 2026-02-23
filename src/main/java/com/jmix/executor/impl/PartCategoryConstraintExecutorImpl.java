package com.jmix.executor.impl;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.SolverResult;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.model.AttrFunConstant;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.InferPartCategoryReq;
import com.jmix.executor.model.ParConstraint;
import com.jmix.executor.model.PartConstraintReq;
import com.jmix.executor.model.Result;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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
public class PartCategoryConstraintExecutorImpl extends ModuleBaseConstraintExecutorImpl {

    public PartCategoryConstraintExecutorImpl(Module module, ConstraintConfig config,
            ModuleAlgClassLoader moduleAlgClassLoader) {
        super(module, config, moduleAlgClassLoader);
    }

    /**
     * 处理部件分类约束
     *
     * @param partCatagoryReq 部件分类请求
     * @return 推理结果
     */
    @Override
    public Result<List<ModuleInst>> doProcess(InferPartCategoryReq partCatagoryReq) {
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
                filteredCategory = filteredCategory.filterClone(partConstraintReq);
                String msg = String.format("Priority-filtered aparts: {%s} by {%s}",
                        filteredCategory.getAllAtomicPartShortString(),
                        partConstraintReq.toShortString());
                log.info(msg);
                if (filteredCategory.getAllAtomicPartShortString().isEmpty()) {
                    return Result.noSolution(msg);
                }
                resolveAddPartConstraint(filteredCategory, partConstraintReq, partConstraintFromReqs);
            }
            SolverResult sr = null;
            // 检查是否有优先级规则，如果有则使用分级求解
            if (filteredCategory.hasPriorityRule()) {
                PartCategory mergedFilteredCategory = buildMergedFilteredCategory(originalCategory,
                        partConstraintFromReqs);
                sr = solveWithPriorityConstraints(mergedFilteredCategory,
                        partCatagoryReq, partConstraintFromReqs);
            } else {
                sr = solveWithOutPriorityConstraints(filteredCategory, partCatagoryReq);
            }
            return Result.success(sr.getSolutions());

        } catch (Exception e) {
            log.error("Failed to process part category constraint", e);
            return Result.failed("Failed to process part category constraint: " + e.getMessage());
        }
    }

    /**
     * 求解约束模型并返回结果
     *
     * @param filteredModuleBase 过滤后的模块基础
     * @param partCatagoryReq    部件分类请求
     * @return 求解结果
     */
    @Override
    protected SolverResult solveWithOutPriorityConstraints(IModule filteredModuleBase,
            InferPartCategoryReq partCatagoryReq) {
        log.info("no Priority-process starting........");
        return invokerSolver(filteredModuleBase, partCatagoryReq, new ArrayList<>(), null);
    }

    /**
     * 主求解方法-带优先级约束
     */
    private SolverResult solveWithPriorityConstraints(
            IModule filteredModuleBase, InferPartCategoryReq partCatagoryReq,
            List<ParConstraint> partConstraintFromReqs) {
        log.info("Priority-process pconstraint-step1 max/min starting........");
        // 步骤1：对每个优先级规则优化求解最优解 minimize(objecFun)
        SolverResult result = invokerSolver(filteredModuleBase, partCatagoryReq, partConstraintFromReqs, null);

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
            result = invokerSolver(filteredModuleBase, partCatagoryReq, partConstraintFromReqs, objectValue);

        } while (execTimes < maxExecTimes
                && result.getSolutions().size() <= availableSolutionNum);
        result.setIterationTimes(execTimes);
        return result;
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
    @Override
    protected void initModelByPriorityConstraints(
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
    @Override
    protected ConstraintAlgImpl createConstraintAlg(long moduleId, String moduleCode) {
        return moduleAlgClassLoader.newConstraintAlg(moduleCode);
    }
}
