package com.jmix.executor.impl;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.RefProgObjSchema;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.RuleUtils;
import com.jmix.executor.cmodel.DiagnosticConstraint;
import com.jmix.executor.cmodel.ErrorInfo;
import com.jmix.executor.cmodel.InstErrorCode;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartCategoryInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.cmodel.SolverResult;
import com.jmix.executor.impl.algmodel.AlgCPModel;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.impl.algmodel.RelaxVar;
import com.jmix.executor.impl.util.ModuleUtils;
import com.jmix.executor.impl.util.ReqUtils;
import com.jmix.executor.model.AlgExecutorException;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.CrossCategoryPartCategoryConstraintReq;
import com.jmix.executor.model.InferParasReq;
import com.jmix.executor.model.InferPartCategoryReq;
import com.jmix.executor.model.ModulePostCalcReq;
import com.jmix.executor.model.ModuleValidateReq;
import com.jmix.executor.model.ModuleValidateResp;
import com.jmix.executor.model.PartCategoryConstraintReq;
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
import java.util.LinkedHashSet;
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
        return processModule(module, partCategoryReq);
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
    public Result<List<ModuleInst>> processModule(Module startModule, InferPartCategoryReq partCategoryReq) {
        try {
            List<CrossCategoryPartCategoryConstraintReq> crossReqs = CrossCategoryConstraintValidator.validate(
                    partCategoryReq.getCrossCategoryConstraintReqs(), startModule);
            Map<String, List<PartConstraintReq>> partConstraintReqMap = normalizePartConstraint(
                    partCategoryReq.getPartConstraintReqs(), startModule);
            // 查找原始部件分类
            FilterCloneResult filterResult = filterClone(startModule, partConstraintReqMap);

            Module filterModule = filterResult.filteredModule();
            log.info("Priority-orignal module: {}", ModuleUtils.toShortString(startModule));
            log.info("Priority-filter module: {}", ModuleUtils.toShortString(filterModule));
            SolverResult sr = solveModuleBranches(filterModule, filterResult, partCategoryReq, crossReqs);
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
            Result<List<ModuleInst>> result = Result.success(sr.getSolutions());
            result.setSolverResult(sr);
            return result;

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

    private SolverResult solveModuleBranches(Module baseModule, FilterCloneResult filterResult,
            InferPartCategoryReq req, List<CrossCategoryPartCategoryConstraintReq> crossReqs) {
        ModuleInput baseInput = toModuleModuleInput(baseModule, filterResult, req, crossReqs);

        SolverResult merged = solveBranch(baseModule, baseInput);
        appendOptionalBranches(baseModule, filterResult, req, crossReqs, 0, new ArrayList<>(), merged,
                getOptionalBranchLimit());
        return merged;
    }

    private int appendOptionalBranches(Module baseModule, FilterCloneResult filterResult,
            InferPartCategoryReq req, List<CrossCategoryPartCategoryConstraintReq> crossReqs, int startIndex,
            List<PartCategoryInputBase> selectedOptionalInputs, SolverResult merged, int remainingBranchCount) {
        if (remainingBranchCount <= 0 || filterResult.optionalPartCategoryInputs().isEmpty()) {
            return remainingBranchCount;
        }
        for (int i = startIndex; i < filterResult.optionalPartCategoryInputs().size(); i++) {
            selectedOptionalInputs.add(filterResult.optionalPartCategoryInputs().get(i));
            SolverResult presentResult = solveOptionalBranch(baseModule, filterResult, req, crossReqs,
                    selectedOptionalInputs);
            merged.getSolutions().addAll(presentResult.getSolutions());
            remainingBranchCount--;
            if (remainingBranchCount <= 0) {
                selectedOptionalInputs.remove(selectedOptionalInputs.size() - 1);
                return remainingBranchCount;
            }
            remainingBranchCount = appendOptionalBranches(baseModule, filterResult, req, crossReqs, i + 1,
                    selectedOptionalInputs, merged, remainingBranchCount);
            selectedOptionalInputs.remove(selectedOptionalInputs.size() - 1);
            if (remainingBranchCount <= 0) {
                return remainingBranchCount;
            }
        }
        return remainingBranchCount;
    }

    private SolverResult solveOptionalBranch(Module baseModule, FilterCloneResult filterResult,
            InferPartCategoryReq req, List<CrossCategoryPartCategoryConstraintReq> crossReqs,
            List<PartCategoryInputBase> optionalInputs) {
        Module presentModule = cloneModuleWithCurrentCategories(baseModule);
        ModuleInput presentInput = toModuleModuleInput(presentModule, filterResult, req, crossReqs);

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

    private ModuleInput toModuleModuleInput(Module module, FilterCloneResult filterResult,
            InferPartCategoryReq req, List<CrossCategoryPartCategoryConstraintReq> crossReqs) {
        ModuleInput input = new ModuleInput();
        input.setModuleId(module.getId());
        input.setModuleCode(module.getCode());
        input.setPreParaInsts(req.getPreParaInsts());
        input.setPrePartInsts(req.getPrePartInsts());
        input.setEnumerateAllSolution(req.isEnumerateAllSolution());
        input.setMaxSolutionNum(req.getMaxSolutionNum());
        input.setRelaxSolve(req.isRelaxSolve());
        input.setPartCategoryInputs(new ArrayList<>(filterResult.partCategoryInputs()));
        input.setPartCategoryErrorInfoMap(filterResult.errorInfoMap());
        input.setCrossCategoryConstraintReqs(new ArrayList<>(crossReqs));
        return input;
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
        if (!hasCategoryConstraintReqs(req)) {
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
            ModuleAlgClassLoader loader = getModuleClassLoader(module.getId());
            if (loader == null) {
                log.error("ModuleAlgClassLoader not found for module: {}", module.getId());
                return Result.failed("ModuleAlgClassLoader not found for module: " + module.getId());
            }
            this.init(module, config, loader);
            return Result.success(validateModuleInstBySolving(module, req.getModuleInst()));
        } catch (AlgLoaderException | AlgExecutorException ex) {
            log.error("Failed to validate module instance", ex);
            return Result.failed("exception: " + ex.getMessage());
        }
    }

    private ModuleValidateResp validateModuleInstBySolving(Module module, ModuleInst moduleInst)
            throws AlgLoaderException, AlgExecutorException {
        ModuleValidateResp resp = new ModuleValidateResp();
        InferParasReq inferReq = toValidationInferReq(module, moduleInst);
        RunInferParasRsp result = runInferParas(module, inferReq, false, new ArrayList<>());
        CpSolverStatus status = result.getStatus();
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            resp.setValid(true);
            resp.setViolatedRuleCodes(new ArrayList<>());
            return resp;
        }
        if (status == CpSolverStatus.INFEASIBLE) {
            resp.setValid(false);
            resp.setViolatedRuleCodes(resolveViolatedRuleCodes(module, inferReq));
            return resp;
        }
        if (status == CpSolverStatus.MODEL_INVALID) {
            ModuleAlgImpl alg = initModuleCpModel(module, inferReq, false, new ArrayList<>());
            String validationError = alg.getModel().getCpModel().validate();
            throw new AlgExecutorException("Model validation failed: " + validationError);
        }
        throw new AlgExecutorException("solver status: " + status);
    }

    private InferParasReq toValidationInferReq(Module module, ModuleInst moduleInst) {
        InferParasReq inferReq = new InferParasReq();
        inferReq.setModuleId(module.getId());
        inferReq.setModuleCode(module.getCode());
        inferReq.setEnumerateAllSolution(false);
        inferReq.setPrePartInsts(toValidationPartInsts(module, moduleInst));
        inferReq.setPreParaInsts(toValidationParaInsts(moduleInst));
        return inferReq;
    }

    private List<PartInst> toValidationPartInsts(Module module, ModuleInst moduleInst) {
        Map<String, Integer> quantityByPartCode = new LinkedHashMap<>();
        for (Part part : module.getAllAtomicParts()) {
            quantityByPartCode.put(part.getCode(), 0);
        }

        List<PartInst> inputParts = moduleInst.getAllParts();
        if (inputParts != null) {
            for (PartInst partInst : inputParts) {
                addValidationPartQuantity(quantityByPartCode, partInst);
            }
        }

        List<PartInst> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quantityByPartCode.entrySet()) {
            PartInst fixedPart = new PartInst();
            fixedPart.setCode(entry.getKey());
            fixedPart.setQuantity(entry.getValue());
            fixedPart.setSelected(entry.getValue() > 0);
            result.add(fixedPart);
        }
        return result;
    }

    private void addValidationPartQuantity(Map<String, Integer> quantityByPartCode, PartInst partInst) {
        if (partInst == null || Strings.isNullOrEmpty(partInst.getCode())) {
            return;
        }
        Integer quantity = validationQuantity(partInst);
        if (quantity == null) {
            quantity = 0;
        }
        quantityByPartCode.put(partInst.getCode(), quantity);
    }

    private Integer validationQuantity(PartInst partInst) {
        Integer quantity = partInst.getQuantity();
        if (partInst.isSelected() && (quantity == null || quantity <= 0)) {
            return 1;
        }
        if (quantity == null) {
            return null;
        }
        if (quantity < 0) {
            throw new AlgLoaderException("Part quantity cannot be negative: " + partInst.getCode());
        }
        return quantity;
    }

    private List<ParaInst> toValidationParaInsts(ModuleInst moduleInst) {
        List<ParaInst> result = new ArrayList<>();
        if (moduleInst.getParas() == null) {
            return result;
        }
        for (ParaInst paraInst : moduleInst.getParas()) {
            if (paraInst == null || Strings.isNullOrEmpty(paraInst.getCode())
                    || Strings.isNullOrEmpty(paraInst.getValue())) {
                continue;
            }
            ParaInst fixedPara = new ParaInst();
            fixedPara.setCode(paraInst.getCode());
            fixedPara.setValue(paraInst.getValue());
            result.add(fixedPara);
        }
        return result;
    }

    private List<String> resolveViolatedRuleCodes(Module module, InferParasReq inferReq)
            throws AlgLoaderException, AlgExecutorException {
        List<RelaxVar> conflictedRelaxs = new ArrayList<>();
        Pair<List<RelaxVar>, RunInferParasRsp> first = runCalcRuleConfictRules(module, inferReq, conflictedRelaxs);
        conflictedRelaxs.addAll(first.getFirst());

        Pair<List<RelaxVar>, RunInferParasRsp> second = runCalcRuleConfictRules(module, inferReq, conflictedRelaxs);
        conflictedRelaxs.addAll(second.getFirst());

        Set<String> violatedCodes = new LinkedHashSet<>();
        for (RelaxVar relaxVar : conflictedRelaxs) {
            String ruleCode = relaxVar.getRuleCode();
            if (isBusinessRuleCode(module, ruleCode)) {
                violatedCodes.add(ruleCode);
            }
        }
        return new ArrayList<>(violatedCodes);
    }

    private Pair<List<RelaxVar>, RunInferParasRsp> runCalcRuleConfictRules(Module module,
            InferParasReq inferReq,
            List<RelaxVar> conflictedRelaxs) throws AlgLoaderException, AlgExecutorException {
        RunInferParasRsp result = runInferParas(module, inferReq, true, conflictedRelaxs, false);
        CpSolverStatus status = result.getStatus();
        if (isFailed(status)) {
            throw new AlgExecutorException("Solver failed with status: " + status);
        }
        if (status == CpSolverStatus.INFEASIBLE) {
            return new Pair<>(new ArrayList<>(), result);
        }
        return new Pair<>(calcConfictRules(result), result);
    }

    private boolean isBusinessRuleCode(Module module, String ruleCode) {
        if (Strings.isNullOrEmpty(ruleCode)) {
            return false;
        }
        if (ruleCode.startsWith("addPartEquality_") || ruleCode.startsWith("addParaEquality_")) {
            return false;
        }
        if (ruleCode.equals("hiddensrule") || ruleCode.startsWith("sys_")) {
            return false;
        }
        return findRuleByCode(module, ruleCode) != null;
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
        if (module.getAllRules() == null) {
            return null;
        }
        for (Rule rule : module.getAllRules()) {
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
        return runInferParas(module, req, isAttachRelax, confictedRelaxs, true);
    }

    private RunInferParasRsp runInferParas(Module module,
            InferParasReq req,
            boolean isAttachRelax,
            List<RelaxVar> confictedRelaxs,
            boolean relaxSystemRules)
            throws AlgLoaderException, AlgExecutorException {
        // 初始化约束模型
        ModuleAlgImpl alg = initModuleCpModel(module, req, isAttachRelax, confictedRelaxs, relaxSystemRules);
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
        return initModuleCpModel(module, req, isAttachRelax, confictedRelaxs, true);
    }

    private ModuleAlgImpl initModuleCpModel(Module module, InferParasReq req,
            boolean isAttachRelax, List<RelaxVar> confictedRelaxs, boolean relaxSystemRules)
            throws AlgLoaderException, AlgExecutorException {
        // 创建约束算法实例
        ModuleAlgImpl alg = createConstraintAlg(module.getId(), module.getCode());
        AlgCPModel model = new AlgCPModel();
        model.setIsAttachRelax(isAttachRelax);
        model.setRelaxSystemRules(relaxSystemRules);
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
        List<PartConstraintReq> singleReqs = normalizeSinglePartCategoryReqs(req);
        Map<String, List<PartConstraintReq>> partConstraintReqMap = normalizePartConstraint(singleReqs,
                startModule);
        FilterCloneResult filterResult = ModuleConstraintExecutorImpl.filterClone(startModule,
                partConstraintReqMap);
        moduleInput.setPartCategoryInputs(filterResult.partCategoryInputs());
        moduleInput.setPartCategoryErrorInfoMap(filterResult.errorInfoMap());
        moduleInput.setCrossCategoryConstraintReqs(CrossCategoryConstraintValidator.validate(
                req.getCrossCategoryConstraintReqs(), startModule));
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
        cReq.setPartConstraintReqs(normalizeSinglePartCategoryReqs(req));
        cReq.setCrossCategoryConstraintReqs(req.getCrossCategoryConstraintReqs());
        cReq.setEnumerateAllSolution(req.isEnumerateAllSolution());
        cReq.setPartCategoryCode(req.getPartCategoryCode());
        cReq.setRelaxSolve(req.isRelaxSolve());
        return cReq;
    }

    private boolean hasCategoryConstraintReqs(InferParasReq req) {
        return (req.getPartConstraintReqs() != null && !req.getPartConstraintReqs().isEmpty())
                || (req.getPartCategoryConstraintReqs() != null
                        && !req.getPartCategoryConstraintReqs().isEmpty())
                || (req.getCrossCategoryConstraintReqs() != null
                        && !req.getCrossCategoryConstraintReqs().isEmpty());
    }

    private List<PartConstraintReq> normalizeSinglePartCategoryReqs(InferParasReq req) {
        List<PartConstraintReq> result = new ArrayList<>();
        if (req.getPartConstraintReqs() != null) {
            result.addAll(req.getPartConstraintReqs());
        }
        if (req.getPartCategoryConstraintReqs() != null) {
            for (PartCategoryConstraintReq partCategoryReq : req.getPartCategoryConstraintReqs()) {
                result.add(toPartConstraintReq(partCategoryReq));
            }
        }
        return result;
    }

    private PartConstraintReq toPartConstraintReq(PartCategoryConstraintReq req) {
        if (req instanceof PartConstraintReq partConstraintReq) {
            return partConstraintReq;
        }
        PartConstraintReq result = new PartConstraintReq();
        result.setPartCategoryCode(req.getPartCategoryCode());
        result.setAttrType(req.getAttrType());
        result.setAttrCode(req.getAttrCode());
        result.setAttrComparator(req.getAttrComparator());
        result.setAttrValue(req.getAttrValue());
        result.setAttrWhereCondition(req.getAttrWhereCondition());
        result.setDecisionStrategies(req.getDecisionStrategies());
        return result;
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
