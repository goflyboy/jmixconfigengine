package com.jmix.executor.impl;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.PriorityRuleSchema;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.RelaxVar;
import com.jmix.executor.model.AttrFunConstant;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.InferPartCategoryReq;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.model.ParConstraint;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.model.PartConstraintReq;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.model.Result;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.LinearExpr;

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

    private final ConstraintConfig config;
    private final ModuleAlgClassLoader moduleAlgClassLoader;

    public PartCategoryConstraintExecutorImpl(ConstraintConfig config, ModuleAlgClassLoader moduleAlgClassLoader) {
        this.config = config;
        this.moduleAlgClassLoader = moduleAlgClassLoader;
    }

    /**
     * 处理部件分类约束
     * 
     * @param partCatagoryReq 部件分类请求
     * @param module          模块对象
     * @param isAttachRelax   是否附加松弛变量
     * @param confictedRelaxs 冲突松弛变量列表
     * @return 推理结果
     */
    public Result<List<ModuleInst>> processPartCategory(InferPartCategoryReq partCatagoryReq, Module module,
            boolean isAttachRelax, List<RelaxVar> confictedRelaxs) {
        try {
            // 创建约束算法实例
            ConstraintAlgImpl alg = createConstraintAlg(module.getId(), module.getCode());

            // 查找原始部件分类
            PartCategory originalCategory = module.findPartCategory(partCatagoryReq.getPartCatagoryCode());
            if (originalCategory == null) {
                log.error("PartCategory not found: {}", partCatagoryReq.getPartCatagoryCode());
                return Result.failed("PartCategory not found: " + partCatagoryReq.getPartCatagoryCode());
            }
            log.info("Priority-orginal aparts: {}", originalCategory.getAtomicPartShortString());
            // 遍历所有部件约束请求，逐步过滤
            PartCategory filteredCategory = originalCategory;
            List<ParConstraint> partConstraintFromReqs = new ArrayList<>();
            for (PartConstraintReq partConstraintReq : partCatagoryReq.getPartConstraintReqs()) {
                // 一条partConstraintReq会被拆分两条规则，一条是过滤规则（根据where条件过滤），一条是约束规则（根据attrCode,
                // attrComparator, attrValue构建约束表达式）
                // 查询满足条件的PartCategory
                filteredCategory = filteredCategory.query(partConstraintReq);
                log.info("Priority-filtered aparts: {} by {}", filteredCategory.getAtomicPartShortString(),
                        partConstraintReq.toShortString());
                resolveAddPartConstraint(filteredCategory, partConstraintReq, partConstraintFromReqs);
            }

            // 检查是否有优先级规则，如果有则使用分级求解
            if (filteredCategory.hasPriorityRule()) {
                PartCategory mergedFilteredCategory = buildMergedFilteredCategory(originalCategory,
                        partConstraintFromReqs);
                return solveWithPriorityConstraints(alg, mergedFilteredCategory, isAttachRelax, confictedRelaxs,
                        partCatagoryReq, module, partConstraintFromReqs);
            }

            return solveConstraintModel(alg, filteredCategory, isAttachRelax, confictedRelaxs, partCatagoryReq, module);

        } catch (Exception e) {
            log.error("Failed to process part category constraint", e);
            return Result.failed("Failed to process part category constraint: " + e.getMessage());
        }
    }

    /**
     * 求解约束模型并返回结果
     * 
     * @param alg              约束算法实现
     * @param filteredCategory 过滤后的部件分类
     * @param isAttachRelax    是否附加松弛变量
     * @param confictedRelaxs  冲突松弛变量列表
     * @param partCatagoryReq  部件分类请求
     * @param module           模块对象
     * @return 求解结果
     */
    private Result<List<ModuleInst>> solveConstraintModel(ConstraintAlgImpl alg, PartCategory filteredCategory,
            boolean isAttachRelax, List<RelaxVar> confictedRelaxs,
            InferPartCategoryReq partCatagoryReq,
            Module module) {

        // 创建CP模型
        CpModel model = new CpModel();
        alg.initModel(model, filteredCategory, isAttachRelax, confictedRelaxs);

        // 根据请求初始化约束模型
        initModelByReq(partCatagoryReq, alg);

        // 添加松弛目标函数, 方便调试
        alg.addRelaxObjectFunction();

        if (config.isLogModelProto()) {
            // 将module的CpModelProto信息输出到文件config.logFilePath/module.proto.txt
            alg.getModel().getCpModel()
                    .exportToFile(config.getLogFilePath() + File.separator + module.getCode() + ".proto.txt");
        }

        CpSolver solver = new CpSolver();
        solver.getParameters().setEnumerateAllSolutions(partCatagoryReq.isEnumerateAllSolution());
        solver.getParameters().setNumSearchWorkers(1); // 单线程搜索，防止有重复解
        log.info("solver parameters:\n" + solver.getParameters().toString());
        // 可按需设置更多参数
        int maxSolutionNum = partCatagoryReq.getMaxSolutionNum() > 0 ? partCatagoryReq.getMaxSolutionNum() : 10;
        ModuleInstSolutionCallBack cb = new ModuleInstSolutionCallBack(module, alg.getVars(),
                alg.getOtherVarMap(), null, null, maxSolutionNum);
        CpSolverStatus status = solver.solve(alg.getModel().getCpModel(), cb);

        // 如果模型无效，调用ValidateCpModel获取详细错误信息
        if (status == CpSolverStatus.MODEL_INVALID) {
            // 重新获取模型进行验证
            ConstraintAlgImpl validationAlg = createConstraintAlg(module.getId(), module.getCode());
            CpModel validationModel = new CpModel();
            validationAlg.initModel(validationModel, module, false, new ArrayList<>());
            String validationError = validationModel.validate();
            log.error("Model validation failed: {}", validationError);
            return Result.failed("Model validation failed: " + validationError);
        }

        if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE
                && status != CpSolverStatus.INFEASIBLE) {
            return Result.failed("solver status: " + status);
        }

        if (status == CpSolverStatus.INFEASIBLE) {
            return Result.success(null);
        }

        // 返回成功结果
        return Result.success(cb.getAllSolutions());
    }

    /**
     * 使用优先级约束进行分级求解
     * 
     * @param alg                    约束算法实现
     * @param filteredCategory       过滤后的部件分类
     * @param isAttachRelax          是否附加松弛变量
     * @param confictedRelaxs        冲突松弛变量列表
     * @param partCatagoryReq        部件分类请求
     * @param module                 模块对象
     * @param partConstraintFromReqs 部件约束列表
     * @return 求解结果
     */
    private Result<List<ModuleInst>> solveWithPriorityConstraints(ConstraintAlgImpl alg,
            PartCategory filteredCategory, boolean isAttachRelax, List<RelaxVar> confictedRelaxs,
            InferPartCategoryReq partCatagoryReq, Module module, List<ParConstraint> partConstraintFromReqs) {
        List<Rule> priorityRules = filteredCategory.queryPriorityRules();
        List<ModuleInst> allSolutions = new ArrayList<>();
        log.info(" Priority-process pconstraint-step1 max/min starting........");

        // 步骤1：对每个优先级规则优化求解最优解
        List<PriorityAttrValueImpl> optimalValues = new ArrayList<>();
        for (Rule rule : priorityRules) {
            if (rule.getRawCode() == null || !(rule.getRawCode() instanceof PriorityRuleSchema)) {
                log.error("Rule rawCode is not PriorityRuleSchema for rule: {}", rule.getCode());
                return Result.failed("Rule rawCode is not PriorityRuleSchema for rule: " + rule.getCode());
            }

            PriorityRuleSchema schema = (PriorityRuleSchema) rule.getRawCode();
            String attrCode = schema.getAttrCode();
            if (attrCode == null || attrCode.isEmpty()) {
                log.error("PriorityRuleSchema attrCode is empty for rule: {}", rule.getCode());
                return Result.failed("PriorityRuleSchema attrCode is empty for rule: " + rule.getCode());
            }

            PriorityStrategy priorityStrategy = schema.getPriorityStrategy();

            log.info("Priority-process pconstraint-step1 max/min for attrCode: {}", attrCode);

            // 创建新的算法实例和模型
            ConstraintAlgImpl optAlg = createConstraintAlg(module.getId(), module.getCode());
            CpModel optModel = new CpModel();
            optAlg.initModel(optModel, filteredCategory, isAttachRelax, confictedRelaxs);
            initModelByReq(partCatagoryReq, optAlg);
            initModelByPriorityConstraints(partConstraintFromReqs, optAlg);
            optAlg.addRelaxObjectFunction();

            // 重新构建目标函数表达式（使用新的alg实例）
            PriorityConstraint pConstraint = optAlg.queryPriorityConstraint(attrCode);
            if (pConstraint == null) {
                log.error("PriorityConstraint not found for attrCode: {}", attrCode);
                return Result.failed("PriorityConstraint not found for attrCode: " + attrCode);
            }
            LinearExpr objectiveExpr = pConstraint.getExpr();

            if (priorityStrategy == PriorityStrategy.MAX) {
                optModel.maximize(objectiveExpr);
            } else {
                optModel.minimize(objectiveExpr);
            }
            PriorityAttrValueImpl objValue = new PriorityAttrValueImpl();
            objValue.setAttrCode(attrCode);
            objValue.setPConstraint(pConstraint);
            // 求解最优解
            CpSolver optSolver = new CpSolver();
            optSolver.getParameters().setNumSearchWorkers(1);
            ModuleInstSolutionCallBack optCb = new ModuleInstSolutionCallBack(module, optAlg.getVars(),
                    optAlg.getOtherVarMap(), optAlg, Arrays.asList(objValue));
            CpSolverStatus optStatus = optSolver.solve(optModel, optCb);

            if (optStatus == CpSolverStatus.OPTIMAL || optStatus == CpSolverStatus.FEASIBLE) {
                double optimalValue = optSolver.objectiveValue();
                objValue.setOptimalValue(optimalValue);
                optimalValues.add(objValue);
                log.info("Priority-process pconstraint-step1 max/min Optimal solution: {}",
                        SolutionUtils.toSolutionString(optCb.getAllSolutions()));
                log.info("Priority-process pconstraint-step1 max/min Optimal value for  {}: {}", attrCode,
                        optimalValue);

            } else {
                log.warn(
                        "Priority-process pconstraint-step1 max/min Failed to find optimal solution for attrCode: {}, status: {}",
                        attrCode, optStatus);
            }
        }
        log.info(" Priority-process pconstraint-step1 max/min finished........");
        // 步骤2：在保持高优先级目标接近最优的前提下，寻找多个解
        if (!optimalValues.isEmpty()) {
            log.info(" Priority-process pconstraint-step2 greater/less starting........");

            // 创建新的算法实例和模型
            ConstraintAlgImpl multiAlg = createConstraintAlg(module.getId(), module.getCode());
            CpModel multiModel = new CpModel();
            multiAlg.initModel(multiModel, filteredCategory, isAttachRelax, confictedRelaxs);
            initModelByReq(partCatagoryReq, multiAlg);
            initModelByPriorityConstraints(partConstraintFromReqs, multiAlg);
            multiAlg.addRelaxObjectFunction();

            // 对每个最优值添加约束：保持高优先级目标在最优值的30%范围内
            double threshold = 0.3;
            for (PriorityAttrValueImpl objValue : optimalValues) {
                String attrCode = objValue.getAttrCode();
                PriorityConstraint pConstraint = objValue.getPConstraint();
                double optimalValue = objValue.getOptimalValue();

                if (attrCode == null || attrCode.isEmpty() || pConstraint == null) {
                    log.error("Invalid PriorityAttrValueImpl, skipping: attrCode={}", attrCode);
                    return Result.failed("Invalid PriorityAttrValueImpl, skipping: attrCode=" + attrCode);
                }
                // 重新构建表达式（使用新的alg实例）
                PriorityConstraint multiPConstraint = multiAlg.queryPriorityConstraint(attrCode);
                if (multiPConstraint == null) {
                    log.error("PriorityConstraint not found for attrCode: {}", attrCode);
                    return Result.failed("PriorityConstraint not found for attrCode: " + attrCode);
                }
                LinearExpr expr = multiPConstraint.getExpr();
                PriorityStrategy priorityStrategy = objValue.getPriorityStrategy();
                if (priorityStrategy == PriorityStrategy.MAX) {
                    double minValue = optimalValue * (1 - threshold);
                    multiModel.addGreaterOrEqual(expr, (long) minValue);
                    log.info(
                            " Priority-process pconstraint-step2 greater/less Added Priority adjusted constraint: {} >= {}",
                            attrCode, (long) minValue);
                } else {
                    double maxValue = optimalValue * (1 + threshold);
                    multiModel.addLessOrEqual(expr, (long) maxValue);
                    log.info(
                            " Priority-process pconstraint-step2 greater/less Added Priority adjusted constraint: {} <= {}",
                            attrCode, (long) maxValue);
                }
            }
            log.info(" Priority-process pconstraint-step2 greater/less finished........");
            log.info(" Priority-process pconstraint-step3 solver starting........");
            // 求解多个解
            CpSolver multiSolver = new CpSolver();
            multiSolver.getParameters().setEnumerateAllSolutions(true);
            multiSolver.getParameters().setNumSearchWorkers(1);

            // 获取最大解数量限制
            int maxSolutionNum = partCatagoryReq.getMaxSolutionNum() > 0 ? partCatagoryReq.getMaxSolutionNum() : 10;
            ModuleInstSolutionCallBack multiCb = new ModuleInstSolutionCallBack(module, multiAlg.getVars(),
                    multiAlg.getOtherVarMap(), multiAlg, optimalValues, maxSolutionNum);
            CpSolverStatus multiStatus = multiSolver.solve(multiModel, multiCb);

            if (multiStatus == CpSolverStatus.OPTIMAL || multiStatus == CpSolverStatus.FEASIBLE) {
                allSolutions.addAll(multiCb.getAllSolutions());
                log.info(" Priority-process pconstraint-step3 solver Found {} solutions ", allSolutions.size());
            } else {
                log.warn(
                        " Priority-process pconstraint-step3 solver Failed to find multiple solutions, status: {}",
                        multiStatus);
            }

        }

        // 步骤3：对解按优先级排序
        if (!allSolutions.isEmpty()) {
            sortSolutionsByPriority(allSolutions, priorityRules, alg);
            log.info(" Priority-process pconstraint-step4 sort solutions by priority finished........");
        }
        log.info(" Priority-process pconstraint-step3 solver finished........");
        return Result.success(allSolutions);
    }

    /**
     * 按优先级对解进行排序
     * 根据solutions中每个ModuleInst.priorityOverallValue进行排序，然后对ModuleInst.prioritySortNo进行赋值
     * 
     * @param solutions     解列表
     * @param priorityRules 优先级规则列表
     * @param alg           约束算法实现
     */
    private void sortSolutionsByPriority(List<ModuleInst> solutions, List<Rule> priorityRules,
            ConstraintAlgImpl alg) {
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
            List<Part> atomicParts = partConstraint.getFilteredCategory().getAtomicParts();
            addPartsToMap(mergedAtomicParts, atomicParts);
        }

        // 判断父分类是否包含在allPartCategoryCodes中
        if (!allPartCategoryCodes.contains(originalCategory.getCode())) {
            // 要把没有包含的子分类的部件补充上
            Map<String, PartCategory> partCategoryMap = originalCategory.getPartCategoryMap();
            for (PartCategory subPartCategory : partCategoryMap.values()) {
                if (!allPartCategoryCodes.contains(subPartCategory.getCode())) {
                    // 补充未包含的子分类的所有原子部件
                    List<Part> subAtomicParts = subPartCategory.getAtomicParts();
                    addPartsToMap(mergedAtomicParts, subAtomicParts);
                }
            }
        }
        // 克隆原始分类并添加合并的部件
        PartCategory resultCategory = originalCategory.clone();
        resultCategory.addParts(new ArrayList<>(mergedAtomicParts.values()));
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
            alg.sumFunConstraint(partConstraint.getFilteredCategory().getAtomicParts(), partConstraint);
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
