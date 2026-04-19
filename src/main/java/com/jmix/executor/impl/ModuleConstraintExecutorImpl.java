package com.jmix.executor.impl;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.RefProgObjSchema;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.RuleUtils;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.SolverResult;
import com.jmix.executor.impl.algmodel.AlgCPModel;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.impl.algmodel.RelaxVar;
import com.jmix.executor.impl.util.ModuleUtils;
import com.jmix.executor.impl.util.ReqUtils;
import com.jmix.executor.model.AlgExecutorException;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.ExtensibleProcess;
import com.jmix.executor.model.InferParasPostProcess;
import com.jmix.executor.model.InferParasReq;
import com.jmix.executor.model.InferPartCategoryReq;
import com.jmix.executor.model.PartConstraintReq;
import com.jmix.executor.model.Result;
import com.jmix.executor.model.RunInferParasRsp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.common.base.Strings;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private final List<ExtensibleProcess> extensibleProcesses = new CopyOnWriteArrayList<>();

    private ConstraintConfig config;

    @Override
    public Result<Void> init(final ConstraintConfig config) {
        this.config = config;
        log.info("Module constraint executor initialized");
        return Result.success(null);
    }

    @Override
    public final Result<Void> fini() {
        // 销毁所有扩展处理器
        for (ExtensibleProcess process : extensibleProcesses) {
            process.destroy();
        }
        extensibleProcesses.clear();
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
            moduleMap.put(m.getId(), m);
            m.init();
            ModuleAlgClassLoader loader = ModuleAlgClassLoader.newInstance(config, m.getAlg());
            loader.init();
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
            ModuleInput moduleInput = new ModuleInput();

            Map<String, List<PartConstraintReq>> partConstraintReqMap = normalizePartConstraint(
                    partCategoryReq.getPartConstraintReqs(), startModule);
            // 查找原始部件分类
            Pair<Module, List<PartCategoryInputBase>> filterResult = filterClone(startModule, partConstraintReqMap);
            moduleInput.setPartCategoryInputs(filterResult.getSecond());

            Module filterModule = filterResult.getFirst();
            log.info("Priority-orignal module: {}", ModuleUtils.toShortString(startModule));
            log.info("Priority-filter module: {}", ModuleUtils.toShortString(filterModule));
            SolverResult sr = null;
            // 检查是否有优先级规则，如果有则使用分级求解
            if (filterModule.hasAllPriorityRule()) {
                sr = solveWithPriorityConstraints(filterModule, moduleInput);
            } else {
                sr = solveWithOutPriorityConstraints(filterModule, moduleInput);
            }
            return Result.success(sr.getSolutions());

        } catch (Exception e) {
            log.error("Failed to process Module constraint", e);
            return Result.failed("Failed to process Module constraint: " + e.getMessage());
        }
    }

    public static Pair<Module, List<PartCategoryInputBase>> filterClone(Module startModule,
            Map<String, List<PartConstraintReq>> partConstraintReqMap) {
        Module result = startModule.clone();
        List<PartCategoryInputBase> partCategoryInputs = new ArrayList<>();
        List<PartConstraintReq> reqs = null;
        PartCategory filterPartCategory = null;
        for (PartCategory partCategory : startModule.getPartCategorys()) {
            reqs = partConstraintReqMap.get(partCategory.getCode());
            if (reqs == null) {
                // 如果是穷举多实例的情况，默认是放进去的，TODO：是不是一个也没有的时候也补充一个
                if (partCategory.isEnumMutiInst()) {
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
                    if (filterPartCategory.getAllAtomicPartShortString().isEmpty()) {
                        throw new AlgExecutorException(msg);
                    }
                    // result.addPart(filterPartCategory);
                    result.addPartCategory(filterPartCategory);
                    PartCategoryInput partCategoryInput = PartCategoryConstraintExecutorImpl
                            .toPartCategoryInput(
                                    filterPartCategory, req);
                    partCategoryInput.setSumAttrParas(partCategory.getAttrParas(AttrParaType.Sum));
                    partCategoryInputs.add(partCategoryInput);
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
                        if (filterPartCategory.getAllAtomicPartShortString().isEmpty()) {
                            throw new AlgExecutorException(msg);
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
                    result.addPartCategory(partCategory); // 放的是全的
                    partCategoryInputs.add(multiInstPartCategoryInput);
                }
            }
        }
        return new Pair<>(result, partCategoryInputs);
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
                ModuleAlgImpl alg = initConstraintModel(module, req, false, new ArrayList<>());
                AlgCPModel model = alg.getModel();
                String validationError = model.getCpModel().validate();
                log.error("Model validation failed: {}", validationError);
                return Result.failed("Model validation failed: " + validationError);
            }

            if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE
                    && status != CpSolverStatus.INFEASIBLE) {
                return Result.failed("solver status: " + status);
            }
            if (status == CpSolverStatus.INFEASIBLE && config.isDebugByRelaxVar()) {
                // 没有可行解，如果 debugByRelaxVar= true，则使用松弛变量检测冲突规则
                Pair<List<RelaxVar>, RunInferParasRsp> rcResult = null;
                List<RelaxVar> confictedRelaxs = new ArrayList<>();

                // 第一次运行
                rcResult = runCalcConfictRules(module, req, true, confictedRelaxs);
                confictedRelaxs.addAll(rcResult.getFirst());

                // 第二次运行，使用第一次的冲突结果
                rcResult = runCalcConfictRules(module, req, true, confictedRelaxs);
                confictedRelaxs.addAll(rcResult.getFirst());

                Result<List<ModuleInst>> r = Result.noSolution(toConfictMessage(confictedRelaxs));

                // 最后一次solution的解
                List<ModuleInst> solutions = rcResult.getSecond().getSolutionCallBack().getSolverResult()
                        .getSolutions();
                r.setData(solutions);
                return r;
            }

            // 执行后处理
            List<ModuleInst> solutions = cb.getSolverResult().getSolutions();
            solutions = executePostProcess(module, solutions);

            return Result.success(solutions);
        } catch (AlgLoaderException | AlgExecutorException ex) {
            log.error("Failed to infer paras", ex);
            return Result.failed("exception: " + ex.getMessage());
        }
    }

    @Override
    public Result<Void> registerExtensible(ExtensibleProcess eProcess) {
        if (eProcess == null) {
            return Result.failed("ExtensibleProcess is null");
        }
        if (extensibleProcesses.contains(eProcess)) {
            log.warn("ExtensibleProcess already registered: {}", eProcess.getProcessName());
            return Result.success(null);
        }
        Result<Void> initResult = eProcess.init();
        if (initResult.getCode() != Result.SUCCESS) {
            return Result.failed("Failed to initialize extensible process: " + initResult.getMessage());
        }
        extensibleProcesses.add(eProcess);
        // 按优先级排序
        extensibleProcesses.sort(Comparator.comparingInt(ExtensibleProcess::getPriority));
        log.info("Registered extensible process: {} with priority: {}",
                eProcess.getProcessName(), eProcess.getPriority());
        return Result.success(null);
    }

    @Override
    public Result<Void> unregisterExtensible(ExtensibleProcess eProcess) {
        if (eProcess == null) {
            return Result.failed("ExtensibleProcess is null");
        }
        if (!extensibleProcesses.contains(eProcess)) {
            log.warn("ExtensibleProcess not registered: {}", eProcess.getProcessName());
            return Result.success(null);
        }
        eProcess.destroy();
        extensibleProcesses.remove(eProcess);
        log.info("Unregistered extensible process: {}", eProcess.getProcessName());
        return Result.success(null);
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
        ModuleAlgImpl alg = initConstraintModel(module, req, isAttachRelax, confictedRelaxs);
        AlgCPModel model = alg.getModel();

        if (config.isLogModelProto()) {
            // 将module的CpModelProto信息输出到文件config.logFilePath/module.proto.txt
            model.getCpModel().exportToFile(config.getLogFilePath() + File.separator + module.getCode() + ".proto.txt");
        }
        CpSolver solver = new CpSolver();
        solver.getParameters().setEnumerateAllSolutions(req.isEnumerateAllSolution());
        solver.getParameters().setNumSearchWorkers(1); // 单线程搜索，防止有重复解
        log.info("solver parameters:\n" + solver.getParameters().toString());
        // 可按需设置更多参数
        ModuleInstSolutionCallBack cb = new ModuleInstSolutionCallBack(module, alg);
        CpSolverStatus status = solver.solve(model.getCpModel(), cb);
        log.info("solver  models:\n");
        model.printModelSummary();
        return new RunInferParasRsp(status, cb, alg.getModel(), solver);
    }

    private ModuleAlgImpl initConstraintModel(Module module, InferParasReq req,
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
        // moduleInput.setPartConstraintReqs(req.getPartConstraintReqs());
        moduleInput.setEnumerateAllSolution(req.isEnumerateAllSolution());
        // moduleInput.setPartCategoryCode(req.getPartCategoryCode());
        Map<String, List<PartConstraintReq>> partConstraintReqMap = normalizePartConstraint(req.getPartConstraintReqs(),
                startModule);
        Pair<Module, List<PartCategoryInputBase>> filterResult = ModuleConstraintExecutorImpl.filterClone(startModule,
                partConstraintReqMap);
        moduleInput.setPartCategoryInputs(filterResult.getSecond());
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
     * 执行后处理
     * 
     * @param module    模块
     * @param solutions 原始解决方案
     * @return 处理后的解决方案
     */
    private List<ModuleInst> executePostProcess(Module module, List<ModuleInst> solutions) {
        List<ModuleInst> result = solutions;

        for (ExtensibleProcess process : extensibleProcesses) {
            if (process instanceof InferParasPostProcess) {
                result = applyPostProcess(module, result, (InferParasPostProcess) process);
            }
        }

        return result;
    }

    /**
     * 应用单个后处理流程
     * 
     * @param module        模块
     * @param currentResult 当前结果
     * @param postProcess   后处理流程
     * @return 处理后的结果
     */
    private List<ModuleInst> applyPostProcess(Module module, List<ModuleInst> currentResult,
            InferParasPostProcess postProcess) {
        Result<List<ModuleInst>> processResult = postProcess.postProcess(module, currentResult);
        if (processResult.getCode() != Result.SUCCESS || processResult.getData() == null) {
            log.warn("Post process failed: {}, message: {}",
                    postProcess.getClass().getSimpleName(), processResult.getMessage());
            return currentResult;
        }
        log.info("Applied post process: {} successfully", postProcess.getClass().getSimpleName());
        return processResult.getData();
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