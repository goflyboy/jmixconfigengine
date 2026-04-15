package com.jmix.executor.impl;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.SolverResult;
import com.jmix.executor.model.InferPartCategoryReq;
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

    public PartCategoryConstraintExecutorImpl() {
    }

    /**
     * 处理部件分类约束
     *
     * @param partCategoryReq 部件分类请求
     * @return 推理结果
     */
    @Override
    public Result<List<ModuleInst>> doProcess(InferPartCategoryReq partCategoryReq) {
        try {
            // 查找原始部件分类
            PartCategory originalCategory = module.getPartCategory(partCategoryReq.getPartCategoryCode());
            if (originalCategory == null) {
                log.error("PartCategory not found: {}", partCategoryReq.getPartCategoryCode());
                return Result.failed("PartCategory not found: " + partCategoryReq.getPartCategoryCode());
            }
            log.info("Priority-orginal aparts: {}", originalCategory.getAllAtomicPartShortString());
            // 遍历所有部件约束请求，逐步过滤
            PartCategory filteredCategory = originalCategory;
            List<PartCategoryInputBase> partCategoryInputs = new ArrayList<>();
            for (PartConstraintReq partConstraintReq : partCategoryReq.getPartConstraintReqs()) {
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
                PartCategoryInput partCategoryInput = PartCategoryConstraintExecutorImpl.toPartCategoryInput(
                        filteredCategory, partConstraintReq);
                partCategoryInputs.add(partCategoryInput);
            }
            ModuleInput moduleInput = new ModuleInput();
            moduleInput.setPartCategoryInputs(partCategoryInputs);
            moduleInput.setPartCategoryReq(partCategoryReq);
            SolverResult sr = null;
            // 检查是否有优先级规则，如果有则使用分级求解
            Module moduleClone = module.clone();

            if (filteredCategory.hasPriorityRule()) {
                PartCategory mergedFilteredCategory = buildMergedFilteredCategory(originalCategory,
                        partCategoryInputs);
                moduleClone.addPartCategory(mergedFilteredCategory);
                sr = solveWithPriorityConstraints(
                        moduleClone, moduleInput);
            } else {
                moduleClone.addPartCategory(filteredCategory);
                sr = solveWithOutPriorityConstraints(moduleClone, moduleInput);
            }
            return Result.success(sr.getSolutions());

        } catch (Exception e) {
            log.error("Failed to process part category constraint", e);
            return Result.failed("Failed to process part category constraint: " + e.getMessage());
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
     * @param originalCategory   原始部件分类
     * @param partCategoryInputs 部件约束列表
     * @return 合并后的过滤分类
     */
    private PartCategory buildMergedFilteredCategory(PartCategory originalCategory,
            List<PartCategoryInputBase> partCategoryInputs) {
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
        for (PartCategoryInputBase ipt : partCategoryInputs) {
            if (!(ipt instanceof PartCategoryInput)) {
                log.warn("ipt is not PartCategoryInput, code: " + ipt.getPartCategoryCode());
                continue;
            }
            PartCategoryInput partConstraint = (PartCategoryInput) ipt;
            String categoryCode = partConstraint.getOrgReq().getPartCategoryCode();
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

}
