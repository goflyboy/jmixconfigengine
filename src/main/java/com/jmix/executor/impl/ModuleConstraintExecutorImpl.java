package com.jmix.executor.impl;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.BusinessRelationType;
import com.jmix.executor.bmodel.logic.CodependantRuleSchema;
import com.jmix.executor.bmodel.logic.CombinationStructRuleSchema;
import com.jmix.executor.bmodel.logic.PairStructRuleSchema;
import com.jmix.executor.bmodel.logic.PartCombination;
import com.jmix.executor.bmodel.logic.PartCombinationType;
import com.jmix.executor.bmodel.logic.RefProgObjSchema;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.RuleSchema;
import com.jmix.executor.bmodel.logic.RuleUtils;
import com.jmix.executor.bmodel.logic.StructCompareOperator;
import com.jmix.executor.bmodel.logic.StructExprSchema;
import com.jmix.executor.bmodel.logic.TripleStructRuleSchema;
import com.jmix.executor.cmodel.DiagnosticConstraint;
import com.jmix.executor.cmodel.ErrorInfo;
import com.jmix.executor.cmodel.InstErrorCode;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.PartCategoryInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.cmodel.SolverResult;
import com.jmix.executor.impl.algmodel.AlgCPModel;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.impl.algmodel.RelaxVar;
import com.jmix.executor.impl.util.FilterExpressionExecutor;
import com.jmix.executor.impl.util.ModuleUtils;
import com.jmix.executor.impl.util.ReqUtils;
import com.jmix.executor.model.AlgExecutorException;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.InferParasReq;
import com.jmix.executor.model.InferPartCategoryReq;
import com.jmix.executor.model.ModulePostCalcReq;
import com.jmix.executor.model.ModuleValidateReq;
import com.jmix.executor.model.ModuleValidateResp;
import com.jmix.executor.model.PartConstraintReq;
import com.jmix.executor.model.PartConstantAttr;
import com.jmix.executor.model.Result;
import com.jmix.executor.model.RunInferParasRsp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.common.base.Strings;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.SatParameters;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 模块约束执行器实现类
 * 实现ModuleConstraintExecutor接口，提供约束求解的核心功能
 * 
 * @since 2025-09-22
 */
@Slf4j
public class ModuleConstraintExecutorImpl extends ModuleBaseConstraintExecutorImpl implements ModuleConstraintExecutor {

    private final Map<Long, Module> moduleMap = new HashMap<>();

    private final Map<Long, ModuleAlgClassLoader> moduleAlgClassLoaderMap = new HashMap<>();

    private ConstraintConfig config;

    @Override
    public Result<Void> init(final ConstraintConfig config) {
        this.config = config;
        log.info("Module constraint executor initialized");
        return Result.success(null);
    }

    @Override
    public final Result<Void> fini() {
        moduleAlgClassLoaderMap.clear();
        moduleMap.clear();
        log.info("Module constraint executor finalized");
        return Result.success(null);
    }

    @Override
    public final Result<Void> addModule(Long rootModuleId, Module... modules) {
        if (modules == null) {
            return Result.failed("modules is null");
        }
        for (Module m : modules) {
            if (m == null) {
                continue;
            }
            if (m.getAlg() == null) {
                return Result.failed("module alg artifact is null for module: " + m.getCode());
            }
            moduleMap.put(m.getId(), m);
            m.init();
            ModuleAlgClassLoader loader = ModuleAlgClassLoader.newInstance(config, m.getAlg());
            try {
                loader.init();
            } catch (AlgLoaderException ex) {
                return Result.failed(ex.getMessage());
            }
            moduleAlgClassLoaderMap.put(m.getId(), loader);
        }
        log.info("Added modules: {}", modules.length);
        return Result.success(null);
    }

    @Override
    public Result<Void> removeModule(Long moduleId) {
        moduleAlgClassLoaderMap.remove(moduleId);
        moduleMap.remove(moduleId);
        log.info("Removed module: {}", moduleId);
        return Result.success(null);
    }

    @Override
    public Result<List<ModuleInst>> doProcess(InferPartCategoryReq partCategoryReq) {
        return processProduct(module, partCategoryReq);
    }

    private Map<String, List<PartConstraintReq>> normalizePartConstraint(List<PartConstraintReq> orgReqs,
            IModule module) {
        Map<String, List<PartConstraintReq>> normalizedReqs = new HashMap<>();
        if (orgReqs == null) {
            return normalizedReqs;
        }
        for (PartConstraintReq orgReq : orgReqs) {
            List<PartConstraintReq> reqs = normalizedReqs.get(orgReq.getPartCategoryCode());
            if (reqs == null) {
                reqs = new ArrayList<>();
                normalizedReqs.put(orgReq.getPartCategoryCode(), reqs);
            }
            normalizedReqs.get(orgReq.getPartCategoryCode()).add(orgReq);
        }

        // 标准化， 1、每个rootCategory只有1个， 2、属于同一个rootCategory的PartConstraintReq，合并在一起
        // 3、原始需求中针对子的要转换为rootCategory
        return normalizedReqs;
    }

    /**
     * 处理部件分类约束
     *
     * @param partCategoryReq 部件分类请求
     * @return 推理结果
     */
    public Result<List<ModuleInst>> processProduct(Module startModule, InferPartCategoryReq partCategoryReq) {
        try {
            Map<String, List<PartConstraintReq>> partConstraintReqMap = normalizePartConstraint(
                    partCategoryReq.getPartConstraintReqs(), startModule);
            // 查找原始部件分类
            FilterCloneResult filterResult = filterClone(startModule, partConstraintReqMap);

            Module filterModule = filterResult.filteredModule();
            log.info("Priority-orignal module: {}", ModuleUtils.toShortString(startModule));
            log.info("Priority-filter module: {}", ModuleUtils.toShortString(filterModule));
            SolverResult sr = solveProductBranches(filterModule, filterResult);
            // 后处理：注入过滤为空的 PartCategoryInst 错误信息
            injectPartCategoryErrors(sr.getSolutions(), filterResult.errorInfoMap());

            if (filterResult.hasError()) {
                // 判断是否所有有约束的分类都过滤为空
                boolean allConstrainedEmpty = filterResult.errorInfoMap().size() == partConstraintReqMap.size();
                if (sr.hasSolution() && !allConstrainedEmpty) {
                    return Result.partialSuccess(sr.getSolutions(),
                            "Partial success: some categories have empty filter results");
                } else {
                    return Result.noSolution("All categories have empty filter results");
                }
            }
            if (!sr.hasSolution()) {
                Result<List<ModuleInst>> r = Result.noSolution("Cannot find solution");
                r.setData(sr.getSolutions());
                r.setSolverResult(sr);
                return r;
            }
            return Result.success(sr.getSolutions());

        } catch (Exception e) {
            log.error("Failed to process Module constraint", e);
            return Result.failed("Failed to process Module constraint: " + e.getMessage());
        }
    }

    public static FilterCloneResult filterClone(Module startModule,
            Map<String, List<PartConstraintReq>> partConstraintReqMap) {
        Module result = startModule.clone();
        List<PartCategoryInputBase> partCategoryInputs = new ArrayList<>();
        List<PartCategoryInputBase> optionalPartCategoryInputs = new ArrayList<>();
        Map<String, ErrorInfo> errorInfoMap = new LinkedHashMap<>();
        List<PartConstraintReq> reqs = null;
        PartCategory filterPartCategory = null;
        for (PartCategory partCategory : startModule.getPartCategorys()) {
            reqs = partConstraintReqMap.get(partCategory.getCode());
            if (reqs == null) {
                // 如果是穷举多实例的情况，默认是放进去的，TODO：是不是一个也没有的时候也补充一个
                if (partCategory.isEnumMutiInst()) {
                    continue;
                }
                if (partCategory.isOptionalSelection()) {
                    optionalPartCategoryInputs.add(toOptionalInput(partCategory));
                    log.info("Defer optional category {} to augmentation branch", partCategory.getCode());
                    continue;
                }
                result.addPart(partCategory); // 完全挪过来
            } else {
                if (reqs.size() == 1) {
                    PartConstraintReq req = reqs.get(0);
                    filterPartCategory = partCategory.filterClone(req);
                    String msg = String.format("Priority-filtered aparts: {%s} by {%s}",
                            filterPartCategory.getAllAtomicPartShortString(),
                            req.getAttrWhereCondition());
                    log.info(msg);
                    if (filterPartCategory.getAllAtomicParts().isEmpty()) {
                        log.warn("Filter empty for category {}: {}", partCategory.getCode(), msg);
                        errorInfoMap.put(partCategory.getCode(),
                                new ErrorInfo(InstErrorCode.FILTER_EMPTY,
                                        "No parts found for condition: " + req.getAttrWhereCondition()
                                                + " in category " + partCategory.getCode()));
                    }
                    PartCategoryInput partCategoryInput = PartCategoryConstraintExecutorImpl
                            .toPartCategoryInput(
                                    filterPartCategory, req);
                    partCategoryInput.setSumAttrParas(partCategory.getAttrParas(AttrParaType.Sum));
                    if (partCategory.isOptionalSelection() && isMentionOnlyOptionalReq(req)) {
                        forcePresentOptionalInput(partCategoryInput);
                        optionalPartCategoryInputs.add(partCategoryInput);
                    } else {
                        result.addPartCategory(filterPartCategory);
                        partCategoryInputs.add(partCategoryInput);
                    }
                } else {
                    Pair<List<Rule>, List<Rule>> rulePair = RuleUtils.splitRules(partCategory.getRules());
                    List<AttrPara> sumsumAttrParas = partCategory.getAttrParas(AttrParaType.SumSum);
                    List<AttrPara> sumAttrParas = partCategory.getAttrParas(AttrParaType.Sum);

                    MultiInstPartCategoryInput multiInstPartCategoryInput = new MultiInstPartCategoryInput();
                    int instId = ModuleInst.DEFAULT_INSTANCE_ID;
                    for (PartConstraintReq req : reqs) {
                        if (req.getAttrType() == AttrParaType.SumSum) { // 总总的要求
                            PartCategoryConstraintExecutorImpl.setPartCategoryInputBase(multiInstPartCategoryInput,
                                    req);
                            continue;
                        }
                        filterPartCategory = partCategory.filterClone(req);
                        String msg = String.format("Priority-filtered aparts: {%s} by {%s}",
                                filterPartCategory.getAllAtomicPartShortString(),
                                req.getAttrWhereCondition());
                        log.info(msg);
                        if (filterPartCategory.getAllAtomicParts().isEmpty()) {
                            log.warn("Filter empty for category {} inst {}: {}",
                                    partCategory.getCode(), instId, msg);
                            errorInfoMap.put(partCategory.getCode() + "_I" + instId,
                                    new ErrorInfo(InstErrorCode.FILTER_EMPTY,
                                            "No parts found for condition: " + req.getAttrWhereCondition()
                                                    + " in category " + partCategory.getCode()
                                                    + " instance " + instId));
                        }
                        filterPartCategory.setRules(rulePair.getFirst());
                        PartCategoryInput partCategoryInput = PartCategoryConstraintExecutorImpl
                                .toPartCategoryInput(
                                        filterPartCategory, req);
                        partCategoryInput.setInstId(instId++);
                        partCategoryInput.setSumAttrParas(sumAttrParas);
                        multiInstPartCategoryInput.addPartCategoryInput(partCategoryInput);
                    }
                    multiInstPartCategoryInput.setAllInstRules(rulePair.getSecond());
                    multiInstPartCategoryInput.setSumSumAttrParas(sumsumAttrParas);
                    if (partCategory.isOptionalSelection()) {
                        optionalPartCategoryInputs.add(multiInstPartCategoryInput);
                    } else {
                        result.addPartCategory(partCategory); // 放的是全的
                        partCategoryInputs.add(multiInstPartCategoryInput);
                    }
                }
            }
        }
        return new FilterCloneResult(result, partCategoryInputs, errorInfoMap, optionalPartCategoryInputs);
    }

    private SolverResult solveProductBranches(Module baseModule, FilterCloneResult filterResult) {
        ModuleInput baseInput = new ModuleInput();
        baseInput.setPartCategoryInputs(new ArrayList<>(filterResult.partCategoryInputs()));
        baseInput.setPartCategoryErrorInfoMap(filterResult.errorInfoMap());

        SolverResult merged = solveBranch(baseModule, baseInput);
        appendOptionalBranches(baseModule, filterResult, 0, new ArrayList<>(), merged,
                getOptionalBranchLimit());
        return merged;
    }

    private int appendOptionalBranches(Module baseModule, FilterCloneResult filterResult, int startIndex,
            List<PartCategoryInputBase> selectedOptionalInputs, SolverResult merged, int remainingBranchCount) {
        if (remainingBranchCount <= 0 || filterResult.optionalPartCategoryInputs().isEmpty()) {
            return remainingBranchCount;
        }
        for (int i = startIndex; i < filterResult.optionalPartCategoryInputs().size(); i++) {
            selectedOptionalInputs.add(filterResult.optionalPartCategoryInputs().get(i));
            SolverResult presentResult = solveOptionalBranch(baseModule, filterResult, selectedOptionalInputs);
            merged.getSolutions().addAll(presentResult.getSolutions());
            remainingBranchCount--;
            if (remainingBranchCount <= 0) {
                selectedOptionalInputs.remove(selectedOptionalInputs.size() - 1);
                return remainingBranchCount;
            }
            remainingBranchCount = appendOptionalBranches(baseModule, filterResult, i + 1,
                    selectedOptionalInputs, merged, remainingBranchCount);
            selectedOptionalInputs.remove(selectedOptionalInputs.size() - 1);
            if (remainingBranchCount <= 0) {
                return remainingBranchCount;
            }
        }
        return remainingBranchCount;
    }

    private SolverResult solveOptionalBranch(Module baseModule, FilterCloneResult filterResult,
            List<PartCategoryInputBase> optionalInputs) {
        Module presentModule = cloneModuleWithCurrentCategories(baseModule);
        ModuleInput presentInput = new ModuleInput();
        presentInput.setPartCategoryInputs(new ArrayList<>(filterResult.partCategoryInputs()));
        presentInput.setPartCategoryErrorInfoMap(filterResult.errorInfoMap());

        for (PartCategoryInputBase optionalInput : optionalInputs) {
            PartCategory presentCategory = toPresentPartCategory(optionalInput);
            if (presentCategory == null) {
                continue;
            }
            presentModule.addPartCategory(presentCategory);
            presentInput.getPartCategoryInputs().add(optionalInput);
        }
        return solveBranch(presentModule, presentInput);
    }

    private int getOptionalBranchLimit() {
        if (config == null || config.getMaxOptionalPartCategoryBranches() <= 0) {
            return 16;
        }
        return config.getMaxOptionalPartCategoryBranches();
    }

    private static Module cloneModuleWithCurrentCategories(Module source) {
        Module clone = source.clone();
        for (PartCategory partCategory : source.getPartCategorys()) {
            clone.addPartCategory(partCategory);
        }
        return clone;
    }

    private SolverResult solveBranch(Module filteredModule, ModuleInput moduleInput) {
        if (filteredModule.hasAllPriorityRule()) {
            return solveWithPriorityConstraints(filteredModule, moduleInput);
        }
        return solveWithOutPriorityConstraints(filteredModule, moduleInput);
    }

    private static PartCategory toPresentPartCategory(PartCategoryInputBase input) {
        if (input instanceof PartCategoryInput partCategoryInput) {
            return partCategoryInput.getFilteredCategory();
        }
        if (input instanceof MultiInstPartCategoryInput multiInput) {
            List<PartCategoryInput> inputs = multiInput.getPartCategoryInputs();
            if (inputs == null || inputs.isEmpty()) {
                return null;
            }
            return inputs.get(0).getFilteredCategory();
        }
        return null;
    }

    private static PartCategoryInput toOptionalInput(PartCategory partCategory) {
        PartCategoryInput input = new PartCategoryInput();
        input.setFilteredCategory(partCategory);
        input.setSumAttrParas(partCategory.getAttrParas(AttrParaType.Sum));
        forcePresentOptionalInput(input);
        return input;
    }

    private static void forcePresentOptionalInput(PartCategoryInputBase input) {
        input.setAttrType(AttrParaType.Sum);
        input.setSumAttrCode(PartConstantAttr.Quantity.getCode());
        input.setComparator(">=");
        input.setLeftValue(1);
    }

    private static boolean isMentionOnlyOptionalReq(PartConstraintReq req) {
        if (req == null) {
            return false;
        }
        return Strings.isNullOrEmpty(req.getAttrCode())
                && Strings.isNullOrEmpty(req.getAttrComparator())
                && (!Strings.isNullOrEmpty(req.getAttrWhereCondition())
                        || (req.getDecisionStrategies() != null && !req.getDecisionStrategies().isEmpty()));
    }

    /**
     * 将过滤为空的 PartCategory 错误信息注入到求解结果的 PartCategoryInst 中
     */
    private static void injectPartCategoryErrors(List<ModuleInst> solutions,
            Map<String, ErrorInfo> errorInfoMap) {
        if (errorInfoMap == null || errorInfoMap.isEmpty() || solutions == null) {
            return;
        }
        for (ModuleInst solution : solutions) {
            for (Map.Entry<String, ErrorInfo> entry : errorInfoMap.entrySet()) {
                String key = entry.getKey();
                ErrorInfo errorInfo = entry.getValue();
                // 检查是否已存在对应的 PartCategoryInst
                boolean found = false;
                for (PartCategoryInst pcInst : solution.getPartCategorys()) {
                    if (pcInst.getCode().equals(key)) {
                        pcInst.setErrorCode(errorInfo.errorCode());
                        pcInst.setErrorMessage(errorInfo.errorMessage());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // 创建新的 PartCategoryInst 承载错误信息
                    PartCategoryInst errorInst = new PartCategoryInst();
                    errorInst.setCode(key);
                    errorInst.setErrorCode(errorInfo.errorCode());
                    errorInst.setErrorMessage(errorInfo.errorMessage());
                    solution.addPartCategoryInst(errorInst);
                }
            }
        }
    }

    @Override
    public Result<List<ModuleInst>> inferParas(InferParasReq req) {
        if (req == null) {
            return Result.failed("req is null");
        }
        if (req.getPartConstraintReqs() == null || req.getPartConstraintReqs().isEmpty()) {
            return inferParasOld(req);
        }
        try {
            // 获取基础数据
            Module module = getModule(req.getModuleId(), req.getModuleCode());
            module.init();

            // 创建部件分类约束执行器
            ModuleAlgClassLoader loader = getModuleClassLoader(module.getId());
            if (loader == null) {
                log.error("ModuleAlgClassLoader not found for module: {}", module.getId());
                return Result.failed("ModuleAlgClassLoader not found for module: " + module.getId());
            }
            ModuleBaseConstraintExecutorImpl baseExecutor = null;
            Result<List<ModuleInst>> result = null;
            InferPartCategoryReq cReq = toInferPartCategoryReq(req);
            if (!Strings.isNullOrEmpty(cReq.getPartCategoryCode())) {
                baseExecutor = new PartCategoryConstraintExecutorImpl();
            } else {
                baseExecutor = this;
            }
            baseExecutor.init(module, config, loader);
            result = baseExecutor.doProcess(cReq);

            return result;
        } catch (AlgLoaderException | AlgExecutorException ex) {
            log.error("Failed to infer paras", ex);
            return Result.failed("exception: " + ex.getMessage());
        }
    }

    @Override
    public boolean validate(ModuleInst moduleInst) {
        if (moduleInst == null || (moduleInst.getId() == null && Strings.isNullOrEmpty(moduleInst.getCode()))) {
            return false;
        }
        ModuleValidateReq req = new ModuleValidateReq();
        req.setModuleId(moduleInst.getId());
        req.setModuleCode(moduleInst.getCode());
        req.setModuleInst(moduleInst);
        Result<ModuleValidateResp> result = validate(req);
        return result.getCode() == Result.SUCCESS && result.getData() != null && result.getData().isValid();
    }

    @Override
    public Result<ModuleValidateResp> validate(ModuleValidateReq req) {
        if (req == null) {
            return Result.failed("req is null");
        }
        if (req.getModuleInst() == null) {
            return Result.failed("moduleInst is null");
        }
        try {
            Module module = getModule(req.getModuleId(), req.getModuleCode());
            module.init();
            ModuleValidateResp resp = new ModuleValidateResp();
            List<String> violatedRuleCodes = validateModuleInst(module, req.getModuleInst());
            resp.setViolatedRuleCodes(violatedRuleCodes);
            resp.setValid(violatedRuleCodes.isEmpty());
            return Result.success(resp);
        } catch (AlgLoaderException ex) {
            log.error("Failed to validate module instance", ex);
            return Result.failed("exception: " + ex.getMessage());
        }
    }

    private List<String> validateModuleInst(Module module, ModuleInst moduleInst) {
        Set<String> selectedCodes = selectedPartCodes(moduleInst);
        List<String> violatedRuleCodes = new ArrayList<>();
        for (Rule rule : module.getAllRules()) {
            if (!Strings.isNullOrEmpty(rule.getParentRuleCode())) {
                continue;
            }
            if (!isStructValidationRule(rule)) {
                continue;
            }
            if (!validateStructRule(module, rule, moduleInst, selectedCodes)) {
                violatedRuleCodes.add(rule.getCode());
            }
        }
        return violatedRuleCodes;
    }

    private List<String> resolveStructExprCodes(Module module, StructExprSchema expr) {
        validateStructExpr(module, expr);
        List<Part> candidates = module.getAllAtomicParts(expr.getObjectCode());
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }
        List<Part> filtered = filterStructParts(candidates, expr);
        return filtered.stream().map(Part::getCode).distinct().toList();
    }

    private List<Part> filterStructParts(List<Part> candidates, StructExprSchema expr) {
        List<String> values = expr.getValues() == null ? List.of() : expr.getValues();
        if (values.isEmpty()) {
            throw new AlgLoaderException("Structured expression values cannot be empty: " + expr.getObjectCode());
        }
        if (expr.getOperator() == StructCompareOperator.IN) {
            Map<String, Part> result = new LinkedHashMap<>();
            for (String value : values) {
                for (Part part : filterStructPartsBySingleValue(candidates, expr, value)) {
                    result.put(part.getCode(), part);
                }
            }
            return new ArrayList<>(result.values());
        }
        if (expr.getOperator() == StructCompareOperator.NOT_IN) {
            List<Part> result = new ArrayList<>(candidates);
            for (String value : values) {
                result = filterStructPartsBySingleValue(result, expr, value);
            }
            return result;
        }
        return filterStructPartsBySingleValue(candidates, expr, values.get(0));
    }

    private List<Part> filterStructPartsBySingleValue(List<Part> candidates, StructExprSchema expr, String value) {
        return FilterExpressionExecutor.doSelect(candidates, toFilterExpr(expr, value));
    }

    private String toFilterExpr(StructExprSchema expr, String value) {
        return switch (expr.getOperator()) {
            case EQ, IN -> expr.getAttrCode() + "=" + value;
            case NE, NOT_IN -> expr.getAttrCode() + "!=" + value;
            case GT -> expr.getAttrCode() + ">" + value;
            case GE -> expr.getAttrCode() + ">=" + value;
            case LT -> expr.getAttrCode() + "<" + value;
            case LE -> expr.getAttrCode() + "<=" + value;
            case LIKE -> expr.getAttrCode() + " like \"" + value + "\"";
            case NOT_LIKE -> expr.getAttrCode() + " not like \"" + value + "\"";
        };
    }

    private void validateStructExpr(Module module, StructExprSchema expr) {
        if (expr == null) {
            throw new AlgLoaderException("Structured expression cannot be null");
        }
        if (Strings.isNullOrEmpty(expr.getObjectCode())) {
            throw new AlgLoaderException("Structured expression objectCode cannot be empty");
        }
        if (module.getPartCategory(expr.getObjectCode()) == null) {
            throw new AlgLoaderException("PartCategory not found for structured expression: "
                    + expr.getObjectCode());
        }
        if (Strings.isNullOrEmpty(expr.getAttrCode())) {
            throw new AlgLoaderException("Structured expression attrCode cannot be empty");
        }
        if (expr.getOperator() == null) {
            throw new AlgLoaderException("Structured expression operator cannot be null");
        }
    }

    private List<PartCombination> expandStructExprs(Module module, List<StructExprSchema> exprs,
            String sourceRuleCode) {
        List<List<String>> partCodeSets = new ArrayList<>();
        for (StructExprSchema expr : exprs) {
            partCodeSets.add(resolveStructExprCodes(module, expr));
        }
        List<PartCombination> result = new ArrayList<>();
        expandStructCartesian(partCodeSets, 0, new ArrayList<>(), sourceRuleCode, result);
        return result;
    }

    private void expandStructCartesian(List<List<String>> partCodeSets, int index, List<String> tuple,
            String sourceRuleCode, List<PartCombination> result) {
        if (index == partCodeSets.size()) {
            result.add(toPartCombination(tuple, sourceRuleCode));
            return;
        }
        for (String partCode : partCodeSets.get(index)) {
            tuple.add(partCode);
            expandStructCartesian(partCodeSets, index + 1, tuple, sourceRuleCode, result);
            tuple.remove(tuple.size() - 1);
        }
    }

    private PartCombination toPartCombination(List<String> tuple, String sourceRuleCode) {
        if (tuple.size() == 2) {
            return new PartCombination(tuple.get(0), tuple.get(1), sourceRuleCode);
        }
        if (tuple.size() == 3) {
            return new PartCombination(tuple.get(0), tuple.get(1), tuple.get(2), sourceRuleCode);
        }
        throw new AlgLoaderException("Unsupported structured rule arity: " + tuple.size());
    }

    private boolean validateStructRule(Module module, Rule rule, ModuleInst moduleInst, Set<String> selectedCodes) {
        if (rule.getExeSchema() instanceof CodependantRuleSchema codependantRuleSchema) {
            return validateCodependantRule(module, codependantRuleSchema, moduleInst, selectedCodes);
        }
        RuleSchema rawCode = rule.getRawCode();
        if (rawCode instanceof PairStructRuleSchema pairSchema) {
            return validatePairStructRule(module, pairSchema, selectedCodes);
        }
        if (rawCode instanceof TripleStructRuleSchema tripleSchema) {
            return validateTripleStructRule(module, tripleSchema, moduleInst, selectedCodes);
        }
        if (rawCode instanceof CombinationStructRuleSchema combinationSchema) {
            return validateCodependantRule(module, expandForValidate(module, rule, combinationSchema),
                    moduleInst, selectedCodes);
        }
        return true;
    }

    private boolean validatePairStructRule(Module module, PairStructRuleSchema schema, Set<String> selectedCodes) {
        BusinessRelationType relationType = schema.getRelationType();
        if (relationType == null) {
            throw new AlgLoaderException("Pair structured rule relationType cannot be null");
        }
        List<String> leftCodes = resolveStructExprCodes(module, schema.getExpr1());
        List<String> rightCodes = resolveStructExprCodes(module, schema.getExpr2());
        boolean leftSelected = intersects(selectedCodes, leftCodes);
        boolean rightSelected = intersects(selectedCodes, rightCodes);
        return switch (relationType) {
            case INCOMPATIBLE -> !(leftSelected && rightSelected);
            case REQUIRES -> !leftSelected || rightSelected;
            case CO_DEPENDENT -> leftSelected == rightSelected;
        };
    }

    private boolean validateTripleStructRule(Module module, TripleStructRuleSchema schema, ModuleInst moduleInst,
            Set<String> selectedCodes) {
        if (schema.getRelationType() != BusinessRelationType.INCOMPATIBLE) {
            throw new AlgLoaderException("Only ternary incompatible structured validation is supported");
        }
        CodependantRuleSchema exeSchema = new CodependantRuleSchema();
        exeSchema.setArity(3);
        exeSchema.setDimensionCategoryCodes(List.of(schema.getExpr1().getObjectCode(),
                schema.getExpr2().getObjectCode(), schema.getExpr3().getObjectCode()));
        exeSchema.setCombinationType(PartCombinationType.BLACK);
        exeSchema.setCombinations(expandStructExprs(module,
                List.of(schema.getExpr1(), schema.getExpr2(), schema.getExpr3()), ""));
        return validateCodependantRule(module, exeSchema, moduleInst, selectedCodes);
    }

    private boolean validateCodependantRule(Module module, CodependantRuleSchema schema, ModuleInst moduleInst,
            Set<String> selectedCodes) {
        if (schema.getCombinationType() == PartCombinationType.WHITE) {
            return validateWhiteList(module, schema, moduleInst, selectedCodes);
        }
        return validateBlackList(schema, selectedCodes);
    }

    private boolean validateWhiteList(Module module, CodependantRuleSchema schema, ModuleInst moduleInst,
            Set<String> selectedCodes) {
        if (!allDimensionsSelected(module, schema.getDimensionCategoryCodes(), moduleInst)) {
            return true;
        }
        for (PartCombination combination : schema.getCombinations()) {
            List<String> codes = combination.getCodes(schema.getArity());
            if (selectedCodes.containsAll(codes)) {
                return true;
            }
        }
        return false;
    }

    private boolean validateBlackList(CodependantRuleSchema schema, Set<String> selectedCodes) {
        for (PartCombination combination : schema.getCombinations()) {
            List<String> codes = combination.getCodes(schema.getArity());
            if (selectedCodes.containsAll(codes)) {
                return false;
            }
        }
        return true;
    }

    private boolean allDimensionsSelected(Module module, List<String> dimensionCategoryCodes, ModuleInst moduleInst) {
        for (String categoryCode : dimensionCategoryCodes) {
            if (!isDimensionSelected(module, categoryCode, moduleInst)) {
                return false;
            }
        }
        return true;
    }

    private boolean isDimensionSelected(Module module, String categoryCode, ModuleInst moduleInst) {
        PartCategory category = module.getPartCategory(categoryCode);
        if (category == null) {
            return false;
        }
        Set<String> categoryPartCodes = category.getAllAtomicParts().stream()
                .map(Part::getCode)
                .collect(java.util.stream.Collectors.toSet());
        for (PartInst partInst : moduleInst.getAllParts()) {
            if (!categoryPartCodes.contains(partInst.getCode())) {
                continue;
            }
            Integer quantity = partInst.getQuantity();
            if (partInst.isSelected() || (quantity != null && quantity > 0)) {
                return true;
            }
        }
        return false;
    }

    private CodependantRuleSchema expandForValidate(Module module, Rule rule, CombinationStructRuleSchema schema) {
        if (rule.getExeSchema() instanceof CodependantRuleSchema codependantRuleSchema) {
            return codependantRuleSchema;
        }
        return expandCombinationForValidate(module, rule, schema);
    }

    private CodependantRuleSchema expandCombinationForValidate(Module module, Rule parentRule,
            CombinationStructRuleSchema schema) {
        Map<String, Rule> rulesByCode = new LinkedHashMap<>();
        for (Rule rule : module.getAllRules()) {
            rulesByCode.put(rule.getCode(), rule);
        }
        Map<String, Rule> subRules = new LinkedHashMap<>();
        if (schema.getSubRuleCodes() != null) {
            for (String subRuleCode : schema.getSubRuleCodes()) {
                if (Strings.isNullOrEmpty(subRuleCode)) {
                    continue;
                }
                Rule subRule = rulesByCode.get(subRuleCode);
                if (subRule == null) {
                    throw new AlgLoaderException("Combination sub rule not found: " + subRuleCode);
                }
                subRules.put(subRule.getCode(), subRule);
            }
        }
        for (Rule rule : rulesByCode.values()) {
            if (parentRule.getCode().equals(rule.getParentRuleCode())) {
                subRules.put(rule.getCode(), rule);
            }
        }
        if (subRules.isEmpty()) {
            throw new AlgLoaderException("Combination rule has no sub rules: " + parentRule.getCode());
        }
        List<String> dimensions = resolveCombinationDimensions(parentRule, schema, subRules);
        int arity = schema.getArity() > 0 ? schema.getArity() : dimensions.size();
        if (arity != 2 && arity != 3) {
            throw new AlgLoaderException("Only binary and ternary structured rules are supported: " + arity);
        }
        if (dimensions.size() != arity) {
            throw new AlgLoaderException("Combination rule dimensions do not match arity: " + parentRule.getCode());
        }
        schema.setArity(arity);
        schema.setDimensionCategoryCodes(new ArrayList<>(dimensions));

        CodependantRuleSchema exeSchema = new CodependantRuleSchema();
        exeSchema.setArity(arity);
        exeSchema.setDimensionCategoryCodes(new ArrayList<>(dimensions));
        exeSchema.setCombinationType(schema.getCombinationType());
        Map<String, PartCombination> combinations = new LinkedHashMap<>();
        for (Rule subRule : subRules.values()) {
            List<StructExprSchema> exprs = exprsOf(subRule.getRawCode());
            validateCombinationSubRule(parentRule, dimensions, arity, subRule, exprs);
            for (PartCombination combination : expandStructExprs(module, exprs, subRule.getCode())) {
                combinations.put(String.join("|", combination.getCodes(arity)), combination);
            }
        }
        exeSchema.setCombinations(new ArrayList<>(combinations.values()));
        ruleSetExeSchema(parentRule, exeSchema);
        return exeSchema;
    }

    private List<String> resolveCombinationDimensions(Rule parentRule, CombinationStructRuleSchema schema,
            Map<String, Rule> subRules) {
        if (schema.getDimensionCategoryCodes() != null && !schema.getDimensionCategoryCodes().isEmpty()) {
            return schema.getDimensionCategoryCodes();
        }
        Rule firstSubRule = subRules.values().stream()
                .findFirst()
                .orElseThrow(() -> new AlgLoaderException("Combination rule has no sub rules: "
                        + parentRule.getCode()));
        return exprsOf(firstSubRule.getRawCode()).stream()
                .map(StructExprSchema::getObjectCode)
                .toList();
    }

    private void validateCombinationSubRule(Rule parentRule, List<String> dimensions, int arity,
            Rule subRule, List<StructExprSchema> exprs) {
        if (exprs.size() != arity) {
            throw new AlgLoaderException("Combination sub rule arity mismatch: " + subRule.getCode());
        }
        for (int i = 0; i < exprs.size(); i++) {
            String actualCategoryCode = exprs.get(i).getObjectCode();
            String expectedCategoryCode = dimensions.get(i);
            if (!expectedCategoryCode.equals(actualCategoryCode)) {
                throw new AlgLoaderException("Combination sub rule dimension mismatch: parent="
                        + parentRule.getCode() + ", sub=" + subRule.getCode());
            }
        }
    }

    private void ruleSetExeSchema(Rule rule, CodependantRuleSchema schema) {
        rule.setExeSchema(schema);
    }

    private List<StructExprSchema> exprsOf(RuleSchema schema) {
        if (schema instanceof PairStructRuleSchema pairSchema) {
            return List.of(pairSchema.getExpr1(), pairSchema.getExpr2());
        }
        if (schema instanceof TripleStructRuleSchema tripleSchema) {
            return List.of(tripleSchema.getExpr1(), tripleSchema.getExpr2(), tripleSchema.getExpr3());
        }
        throw new AlgLoaderException("Combination sub rule must be pair or triple structured rule");
    }

    private boolean isStructValidationRule(Rule rule) {
        if (rule == null) {
            return false;
        }
        RuleSchema rawCode = rule.getRawCode();
        return rule.getExeSchema() instanceof CodependantRuleSchema
                || rawCode instanceof PairStructRuleSchema
                || rawCode instanceof TripleStructRuleSchema
                || rawCode instanceof CombinationStructRuleSchema;
    }

    private Set<String> selectedPartCodes(ModuleInst moduleInst) {
        Set<String> selectedCodes = new HashSet<>();
        if (moduleInst.getAllParts() == null) {
            return selectedCodes;
        }
        for (PartInst partInst : moduleInst.getAllParts()) {
            Integer qty = partInst.getQuantity();
            if (partInst.isSelected() || (qty != null && qty > 0)) {
                selectedCodes.add(partInst.getCode());
            }
        }
        return selectedCodes;
    }

    private boolean intersects(Set<String> selectedCodes, List<String> candidates) {
        for (String candidate : candidates) {
            if (selectedCodes.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private Result<List<ModuleInst>> inferParasOld(InferParasReq req) {
        if (req == null) {
            return Result.failed("req is null");
        }
        try {
            // 获取基础数据
            Module module = getModule(req.getModuleId(), req.getModuleCode());
            module.init();

            // 获取模块算法类加载器并初始化执行器
            ModuleAlgClassLoader loader = getModuleClassLoader(module.getId());
            if (loader == null) {
                log.error("ModuleAlgClassLoader not found for module: {}", module.getId());
                return Result.failed("ModuleAlgClassLoader not found for module: " +
                        module.getId());
            }
            this.init(module, config, loader);

            // 执行约束推理
            RunInferParasRsp result = runInferParas(module, req, false, new ArrayList<>());
            CpSolverStatus status = result.getStatus();
            ModuleInstSolutionCallBack cb = result.getSolutionCallBack();

            // 如果模型无效，调用ValidateCpModel获取详细错误信息
            if (status == CpSolverStatus.MODEL_INVALID) {
                // 重新获取模型进行验证
                ModuleAlgImpl alg = initModuleCpModel(module, req, false, new ArrayList<>());
                AlgCPModel model = alg.getModel();
                String validationError = model.getCpModel().validate();
                log.error("Model validation failed: {}", validationError);
                return Result.failed("Model validation failed: " + validationError);
            }

            if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE
                    && status != CpSolverStatus.INFEASIBLE) {
                return Result.failed("solver status: " + status);
            }

            // 原始模型无解时的松弛诊断: 请求级开关优先于全局配置
            boolean effectiveRelax = req.isRelaxSolve() || config.isDebugByRelaxVar();
            if (status == CpSolverStatus.INFEASIBLE && effectiveRelax) {
                // 没有可行解，使用松弛变量检测冲突规则
                Pair<List<RelaxVar>, RunInferParasRsp> rcResult = null;
                List<RelaxVar> confictedRelaxs = new ArrayList<>();

                // 第一次运行
                rcResult = runCalcConfictRules(module, req, true, confictedRelaxs);
                confictedRelaxs.addAll(rcResult.getFirst());

                // 第二次运行，使用第一次的冲突结果
                rcResult = runCalcConfictRules(module, req, true, confictedRelaxs);
                confictedRelaxs.addAll(rcResult.getFirst());

                // 最后一次solution的解
                List<ModuleInst> solutions = rcResult.getSecond().getSolutionCallBack().getSolverResult()
                        .getSolutions();

                // 构建冲突诊断约束
                List<DiagnosticConstraint> diagnosticConstraints = toDiagnosticConstraints(confictedRelaxs, module);

                // 构建松弛后的SolverResult
                SolverResult relaxedSr = new SolverResult();
                relaxedSr.setSolutions(solutions);
                relaxedSr.setStrictFeasible(false);
                relaxedSr.setOriginalSolverStatus(CpSolverStatus.INFEASIBLE.toString());
                relaxedSr.setSolverStatus(rcResult.getSecond().getStatus().toString());
                relaxedSr.setDiagnosticConstraints(diagnosticConstraints);

                String message = buildConflictMessage(confictedRelaxs, diagnosticConstraints, solutions.isEmpty());
                relaxedSr.setMessage(message);

                Result<List<ModuleInst>> r = Result.noSolution(message);
                r.setData(solutions);
                r.setSolverResult(relaxedSr);
                return r;
            }

            // 执行后处理: 对每个解执行CalcStage.POST规则
            List<ModuleInst> solutions = cb.getSolverResult().getSolutions();
            ModulePostCalculator postCalculator = new ModulePostCalculator(module, cb.getModuleAlg());
            Result<List<ModuleInst>> postResult = postCalculator.doCalc(solutions);
            if (postResult.getCode() != Result.SUCCESS) {
                return postResult;
            }

            return Result.success(postResult.getData());
        } catch (AlgLoaderException | AlgExecutorException ex) {
            log.error("Failed to infer paras", ex);
            return Result.failed("exception: " + ex.getMessage());
        }
    }

    @Override
    public Result<List<ModuleInst>> postCalculate(ModulePostCalcReq req) {
        if (req == null) {
            return Result.failed("ModulePostCalcReq is null");
        }
        if (req.getSolutions() == null || req.getSolutions().isEmpty()) {
            return Result.failed("solutions is null or empty");
        }
        try {
            Module module = getModule(req.getModuleId(), req.getModuleCode());
            module.init();
            ModuleAlgClassLoader loader = getModuleClassLoader(module.getId());
            if (loader == null) {
                return Result.failed("ModuleAlgClassLoader not found for module: " + module.getId());
            }
            ModuleAlgImpl alg = loader.newConstraintAlg(module.getCode());
            ModulePostCalculator postCalculator = new ModulePostCalculator(module, alg);
            return postCalculator.doCalc(req.getSolutions());
        } catch (AlgLoaderException ex) {
            log.error("Failed to post calculate", ex);
            return Result.failed("exception: " + ex.getMessage());
        }
    }

    /**
     * 根据模块ID获取模块算法类加载器
     * 
     * @param moduleId 模块ID
     * @return 找到的ModuleAlgClassLoader对象，如果未找到则返回null
     */
    private ModuleAlgClassLoader getModuleClassLoader(Long moduleId) {
        return moduleAlgClassLoaderMap.get(moduleId);
    }

    /**
     * 计算冲突规则
     *
     * @param result 运行推理参数响应
     * @return 冲突松弛变量列表
     */
    private List<RelaxVar> calcConfictRules(RunInferParasRsp result) {
        AlgCPModel relaxModel = result.getAlgCPModel();
        // 获取冲突规则信息
        List<RelaxVar> confictedRelaxs = new ArrayList<>();
        Map<String, RelaxVar> relaxVarMap = relaxModel.getRelaxVarMap();
        for (Map.Entry<String, RelaxVar> entry : relaxVarMap.entrySet()) {
            RelaxVar relaxVar = entry.getValue();
            if (result.getSolver().booleanValue(relaxVar.getValue())) {
                confictedRelaxs.add(relaxVar);
            }
        }
        return confictedRelaxs;
    }

    /**
     * 将冲突松弛变量列表转换为冲突消息字符串
     *
     * @param confictedRelaxs 冲突松弛变量列表
     * @return 冲突消息字符串
     */
    private String toConfictMessage(List<RelaxVar> confictedRelaxs) {
        StringBuilder conflictMessage = new StringBuilder("conflict rules: ");
        for (RelaxVar confictedRelax : confictedRelaxs) {
            conflictMessage.append(confictedRelax.getRuleCode()).append(",");
        }
        return conflictMessage.toString();
    }

    /**
     * 将冲突松弛变量列表转换为 DiagnosticConstraint 列表。
     */
    private List<DiagnosticConstraint> toDiagnosticConstraints(List<RelaxVar> confictedRelaxs, Module module) {
        List<DiagnosticConstraint> result = new ArrayList<>();
        for (RelaxVar relaxVar : confictedRelaxs) {
            String ruleCode = relaxVar.getRuleCode();
            DiagnosticConstraint dc = new DiagnosticConstraint();
            dc.setCode(ruleCode);
            dc.setWeight(relaxVar.getWeight());

            if (ruleCode.startsWith("addPartEquality_") || ruleCode.startsWith("addParaEquality_")) {
                dc.setConstraintType(DiagnosticConstraint.TYPE_INPUT);
                dc.setNaturalCode("User input constraint");
                dc.setSource("Para/Part");
                dc.setDescription("User input constraint: " + ruleCode);
            } else if (ruleCode.equals("hiddensrule") || ruleCode.startsWith("sys_")) {
                dc.setConstraintType(DiagnosticConstraint.TYPE_SYSTEM);
                dc.setNaturalCode("System constraint");
                dc.setSource("System");
                dc.setDescription("System-generated constraint: " + ruleCode);
            } else {
                dc.setConstraintType(DiagnosticConstraint.TYPE_RULE);
                // 查找对应的 Rule 获取 naturalCode
                Rule matchedRule = findRuleByCode(module, ruleCode);
                if (matchedRule != null && matchedRule.getNormalNaturalCode() != null) {
                    dc.setNaturalCode(matchedRule.getNormalNaturalCode());
                } else {
                    dc.setNaturalCode(ruleCode);
                }
                dc.setSource(module.getCode() != null ? module.getCode() : "Module");
                dc.setDescription("Business rule " + ruleCode + " was relaxed to find a partial solution");
            }
            result.add(dc);
        }
        return result;
    }

    /**
     * 根据 ruleCode 在 Module 中查找对应的 Rule。
     */
    private Rule findRuleByCode(Module module, String ruleCode) {
        if (module.getRules() == null) {
            return null;
        }
        for (Rule rule : module.getRules()) {
            if (ruleCode.equals(rule.getCode())) {
                return rule;
            }
        }
        return null;
    }

    /**
     * 构建冲突诊断消息，说明原始无解原因、被放宽的约束及部分解信息。
     */
    private static String buildConflictMessage(List<RelaxVar> confictedRelaxs,
            List<DiagnosticConstraint> diagnosticConstraints, boolean relaxedStillInfeasible) {
        if (relaxedStillInfeasible) {
            return "Relaxed model is still infeasible after relaxing: "
                    + diagnosticConstraints.stream()
                            .map(DiagnosticConstraint::getCode)
                            .reduce((a, b) -> a + ", " + b).orElse("");
        }
        StringBuilder msg = new StringBuilder();
        msg.append("Original model infeasible: ");
        List<String> conflictCodes = confictedRelaxs.stream()
                .map(RelaxVar::getRuleCode)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        msg.append(String.join("/", conflictCodes));
        msg.append(" conflict; system relaxed ").append(conflictCodes.size() > 1 ? "them" : "it");
        msg.append(" to find a partial solution");
        return msg.toString();
    }

    private Pair<List<RelaxVar>, RunInferParasRsp> runCalcConfictRules(Module module,
            InferParasReq req,
            boolean isAttachRelax,
            List<RelaxVar> confictedRelaxs) throws AlgExecutorException {

        RunInferParasRsp result = runInferParas(module, req, isAttachRelax, confictedRelaxs);
        CpSolverStatus status = result.getStatus();
        if (isFailed(status)) {
            throw new AlgExecutorException("Solver failed with status: " + status);
        }
        List<RelaxVar> newConfictedRelaxs = calcConfictRules(result);
        return new Pair<>(newConfictedRelaxs, result);

    }

    /**
     * 检查求解器状态是否失败
     *
     * @param status 求解器状态
     * @return 是否失败
     */
    private boolean isFailed(CpSolverStatus status) {
        return status != CpSolverStatus.OPTIMAL
                && status != CpSolverStatus.FEASIBLE
                && status != CpSolverStatus.INFEASIBLE;
    }

    /**
     * 执行约束推理
     * 
     * @param module          模块对象
     * @param req             参数反推请求
     * @param isAttachRelax   是否附加松弛变量
     * @param confictedRelaxs 冲突松弛变量列表
     * @return 包含求解器状态和回调对象的RunInferParasRsp
     * @throws AlgLoaderException   当算法加载器未找到时抛出
     * @throws AlgExecutorException 当创建算法执行时抛出
     */
    private RunInferParasRsp runInferParas(Module module,
            InferParasReq req,
            boolean isAttachRelax,
            List<RelaxVar> confictedRelaxs)
            throws AlgLoaderException, AlgExecutorException {
        // 初始化约束模型
        ModuleAlgImpl alg = initModuleCpModel(module, req, isAttachRelax, confictedRelaxs);
        AlgCPModel model = alg.getModel();

        if (config.isLogModelProto()) {
            // 将module的CpModelProto信息输出到文件config.logFilePath/module.proto.txt
            model.getCpModel().exportToFile(config.getLogFilePath() + File.separator + module.getCode() + ".proto.txt");
        }
        CpSolver solver = new CpSolver();
        solver.getParameters().setEnumerateAllSolutions(req.isEnumerateAllSolution());
        solver.getParameters().setNumSearchWorkers(1); // 单线程搜索，防止有重复解
        if (model.hasDecisionStrategies()) {
            solver.getParameters().setSearchBranching(SatParameters.SearchBranching.FIXED_SEARCH);
            log.info("FIXED_SEARCH enabled for decision strategies");
        }
        log.info("solver parameters:\n" + solver.getParameters().toString());
        // 可按需设置更多参数
        ModuleInstSolutionCallBack cb = new ModuleInstSolutionCallBack(module, alg);
        CpSolverStatus status = solver.solve(model.getCpModel(), cb);
        log.info("solver  models:\n");
        model.printModelSummary();
        return new RunInferParasRsp(status, cb, alg.getModel(), solver);
    }

    private ModuleAlgImpl initModuleCpModel(Module module, InferParasReq req,
            boolean isAttachRelax, List<RelaxVar> confictedRelaxs)
            throws AlgLoaderException, AlgExecutorException {
        // 创建约束算法实例
        ModuleAlgImpl alg = createConstraintAlg(module.getId(), module.getCode());
        AlgCPModel model = new AlgCPModel();
        model.setIsAttachRelax(isAttachRelax);
        model.setConfictedRelaxVars(confictedRelaxs);
        // 根据loadType决定是否使用差量加载模型
        if (config.getLoadType() == ConstraintConfig.LOAD_TYPE_INCREMENTAL) {
            // 差量加载模型
            List<String> inputProgObjs = ReqUtils.buildInputProgObjs(req);

            // 查询子图
            Pair<List<String>, List<RefProgObjSchema>> relativePair = module
                    .querySubGraph(inputProgObjs.toArray(new String[0]));

            // 打印relativePair.first,relativePair.second, 本次推理涉及到exeRules和exeProgObjs
            log.info("Incremental loading - involved rules: {}", relativePair.getFirst());
            log.info("Incremental loading - involved progObjs: {}",
                    relativePair.getSecond().stream().map(RefProgObjSchema::getProgObjCode)
                            .collect(java.util.stream.Collectors.toList()));

            // 调用新的initModel方法,issue:差量后续实现
            alg.init(model, module, toModuleInput(module, req));
        } else {
            // 全量加载模型
            alg.init(model, module, toModuleInput(module, req));
        }

        // // 根据请求初始化约束模型
        // initModelByReq(req, alg);

        // 添加松弛目标函数, 方便调试
        alg.addRelaxObjectFunction();
        return alg;
    }

    private ModuleInput toModuleInput(Module startModule, InferParasReq req) {
        ModuleInput moduleInput = new ModuleInput();
        moduleInput.setModuleId(startModule.getId());
        moduleInput.setModuleCode(startModule.getCode());
        moduleInput.setMainPartInst(req.getMainPartInst());
        moduleInput.setPreParaInsts(req.getPreParaInsts());
        moduleInput.setPrePartInsts(req.getPrePartInsts());
        moduleInput.setRelaxSolve(req.isRelaxSolve());
        // moduleInput.setPartConstraintReqs(req.getPartConstraintReqs());
        moduleInput.setEnumerateAllSolution(req.isEnumerateAllSolution());
        // moduleInput.setPartCategoryCode(req.getPartCategoryCode());
        Map<String, List<PartConstraintReq>> partConstraintReqMap = normalizePartConstraint(req.getPartConstraintReqs(),
                startModule);
        FilterCloneResult filterResult = ModuleConstraintExecutorImpl.filterClone(startModule,
                partConstraintReqMap);
        moduleInput.setPartCategoryInputs(filterResult.partCategoryInputs());
        moduleInput.setPartCategoryErrorInfoMap(filterResult.errorInfoMap());
        return moduleInput;
    }

    /**
     * 将InferParasReq转换为InferPartCategoryReq
     * 
     * @param req 参数反推请求
     * @return 部件分类请求
     */
    private InferPartCategoryReq toInferPartCategoryReq(InferParasReq req) {
        InferPartCategoryReq cReq = new InferPartCategoryReq();
        cReq.setPreParaInsts(req.getPreParaInsts());
        cReq.setPrePartInsts(req.getPrePartInsts());
        cReq.setPartConstraintReqs(req.getPartConstraintReqs());
        cReq.setEnumerateAllSolution(req.isEnumerateAllSolution());
        cReq.setPartCategoryCode(req.getPartCategoryCode());
        cReq.setRelaxSolve(req.isRelaxSolve());
        return cReq;
    }

    /**
     * 根据模块ID或模块代码获取模块对象
     * 
     * @param moduleId   模块ID，可为null
     * @param moduleCode 模块代码，可为null
     * @return 找到的模块对象，如果未找到则返回null
     */
    private Module getModule(Long moduleId, String moduleCode) {
        if (moduleId != null) {
            return moduleMap.get(moduleId);
        }
        if (moduleCode != null) {
            for (Module m : moduleMap.values()) {
                if (moduleCode.equals(m.getCode())) {
                    return m;
                }
            }
        }
        log.error("Module not found: moduleId={}, moduleCode={}", moduleId, moduleCode);
        throw new AlgLoaderException(
                String.format("Module not found: moduleId=%s, moduleCode=%s", moduleId, moduleCode));
    }

    /**
     * 将ModuleInst对象转换为JSON字符串
     * 
     * @param inst ModuleInst实例
     * @return JSON字符串
     */
    public static String toJson(ModuleInst inst) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            return mapper.writeValueAsString(inst);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Serialization failed: " + e.getMessage() + "\"}";
        }
    }

}
