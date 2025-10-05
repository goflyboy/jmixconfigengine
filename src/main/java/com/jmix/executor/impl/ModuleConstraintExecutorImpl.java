package com.jmix.executor.impl;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.rule.RefProgObjSchema;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.util.Pair;
import com.jmix.executor.omodel.AlgExecutorException;
import com.jmix.executor.omodel.AlgLoaderException;
import com.jmix.executor.omodel.ExtensibleProcess;
import com.jmix.executor.omodel.InferParasPostProcess;
import com.jmix.executor.omodel.InferParasReq;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.ParaInst;
import com.jmix.executor.omodel.PartInst;
import com.jmix.executor.omodel.Result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.ortools.sat.CpModel;
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
public class ModuleConstraintExecutorImpl implements ModuleConstraintExecutor {

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
    public Result<List<ModuleInst>> inferParas(InferParasReq req) {
        if (req == null) {
            return Result.failed("req is null");
        }
        try {
            // 获取基础数据
            Module module = getModule(req.getModuleId(), req.getModuleCode());
            module.init();

            // 初始化约束模型
            ConstraintAlgImpl alg = initConstraintModel(module, req);
            CpModel model = alg.getModel().getCpModel();

            if (config.isLogModelProto()) {
                // 将module的CpModelProto信息输出到文件config.logFilePath/module.proto.txt
                model.exportToFile(config.getLogFilePath() + File.separator + module.getCode() + ".proto.txt");
            }
            CpSolver solver = new CpSolver();
            solver.getParameters().setEnumerateAllSolutions(req.isEnumerateAllSolution());
            solver.getParameters().setNumSearchWorkers(1); // 单线程搜索，防止有重复解
            log.info("solver parameters:\n" + solver.getParameters().toString());
            // 可按需设置更多参数
            ModuleInstSolutionCallBack cb = new ModuleInstSolutionCallBack(module, alg.getVars(), alg.getOtherVarMap());
            CpSolverStatus status = solver.solve(model, cb);

            // 如果模型无效，调用ValidateCpModel获取详细错误信息
            if (status == CpSolverStatus.MODEL_INVALID) {
                String validationError = model.validate();
                log.error("Model validation failed: {}", validationError);
                return Result.failed("Model validation failed: " + validationError);
            }

            if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE
                    && status != CpSolverStatus.INFEASIBLE) {
                return Result.failed("solver status: " + status);
            }

            // 执行后处理
            List<ModuleInst> solutions = cb.getAllSolutions();
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
     * 初始化约束模型
     * 
     * @param module 模块对象
     * @param req    参数反推请求
     * @return 初始化完成的约束算法实现
     * @throws AlgLoaderException   当算法加载器未找到时抛出
     * @throws AlgExecutorException 当创建算法执行时抛出
     */
    private ConstraintAlgImpl initConstraintModel(Module module, InferParasReq req)
            throws AlgLoaderException, AlgExecutorException {
        // 加载算法类
        ModuleAlgClassLoader loader = getModuleClassLoader(module.getId());
        if (loader == null) {
            log.error("ModuleAlgClassLoader not found for module: {}", module.getId());
            throw new AlgLoaderException("ModuleAlgClassLoader not found for module: " + module.getId());
        }

        // 初始化约束模型
        ConstraintAlgImpl alg = loader.newConstraintAlg(module.getCode());
        CpModel model = new CpModel();

        // 根据loadType决定是否使用差量加载模型
        if (config.getLoadType() == ConstraintConfig.LOAD_TYPE_INCREMENTAL) {
            // 差量加载模型
            List<String> inputProgObjs = buildInputProgObjs(req);

            // 查询子图
            Pair<List<String>, List<RefProgObjSchema>> relativePair = module
                    .querySubGraph(inputProgObjs.toArray(new String[0]));

            // 打印relativePair.first,relativePair.second, 本次推理涉及到exeRules和exeProgObjs
            log.info("Incremental loading - involved rules: {}", relativePair.getFirst());
            log.info("Incremental loading - involved progObjs: {}",
                    relativePair.getSecond().stream().map(RefProgObjSchema::getProgObjCode)
                            .collect(java.util.stream.Collectors.toList()));

            // 调用新的initModel方法
            alg.initModel(model, module, relativePair.getFirst(), relativePair.getSecond());
        } else {
            // 全量加载模型
            alg.initModel(model, module);
        }

        // 根据请求初始化约束模型
        initModelByReq(req, alg);

        return alg;
    }

    /**
     * 构建输入编程对象列表
     * 
     * @param req 参数反推请求
     * @return 输入编程对象代码列表
     */
    private List<String> buildInputProgObjs(InferParasReq req) {
        List<String> inputProgObjs = new ArrayList<>();

        // 根据req.mainPartInst、preParaInsts、prePartInsts构建inputProgObjs（三个结合相加)
        if (req.getMainPartInst() != null) {
            inputProgObjs.add(req.getMainPartInst().getCode());
        }
        if (req.getPreParaInsts() != null) {
            for (ParaInst paraInst : req.getPreParaInsts()) {
                inputProgObjs.add(paraInst.getCode());
            }
        }
        if (req.getPrePartInsts() != null) {
            for (PartInst partInst : req.getPrePartInsts()) {
                inputProgObjs.add(partInst.getCode());
            }
        }

        return inputProgObjs;
    }

    /**
     * 根据请求初始化约束模型
     * 
     * @param req 参数反推请求
     * @param alg 约束算法实现
     */
    private void initModelByReq(InferParasReq req, ConstraintAlgImpl alg) {
        if (req.getMainPartInst() != null) {
            alg.addPartEquality(req.getMainPartInst().getCode(), req.getMainPartInst().getQuantity());
        }
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