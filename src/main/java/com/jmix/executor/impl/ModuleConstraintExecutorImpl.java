package com.jmix.executor.impl;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.rule.RefProgObjSchema;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.OtherVar;
import com.jmix.executor.impl.algmodel.ParaVar;
import com.jmix.executor.impl.algmodel.PartVar;
import com.jmix.executor.impl.algmodel.Var;
import com.jmix.executor.impl.util.Pair;
import com.jmix.executor.omodel.ExtensibleProcess;
import com.jmix.executor.omodel.InferParasPostProcess;
import com.jmix.executor.omodel.InferParasReq;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.ParaInst;
import com.jmix.executor.omodel.PartInst;
import com.jmix.executor.omodel.Result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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
            try {
                process.destroy();
            } catch (Exception e) {
                log.warn("Failed to destroy extensible process: {}", process.getProcessName(), e);
            }
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
            ModuleAlgClassLoader loader = new ModuleAlgClassLoader(config != null && config.isAttachedDebug(),
                    config != null ? config.getRootFilePath() : null);
            loader.init(m.getCode(), m.getAlg());
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
        Module module = null;
        if (req.getModuleId() != null) {
            module = moduleMap.get(req.getModuleId());
        } else if (req.getModuleCode() != null) {
            for (Module m : moduleMap.values()) {
                if (req.getModuleCode().equals(m.getCode())) {
                    module = m;
                    break;
                }
            }
        }
        if (module == null) {
            return Result.failed("module not found");
        }
        ModuleAlgClassLoader loader = moduleAlgClassLoaderMap.get(module.getId());
        if (loader == null) {
            return Result.failed("loader not found");
        }

        try {
            ConstraintAlgImpl alg = loader.newConstraintAlg(module.getCode());
            CpModel model = new CpModel();
            module.init();

            // 根据loadType决定是否使用差量加载模型
            if (config != null && config.getLoadType() == 0) {
                // 差量加载模型
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
            if (config.isLogModelProto()) {
                // 将module的CpModelProto信息输出到文件config.logFilePath/module.proto.txt
                model.exportToFile(config.getLogFilePath() + "/" + module.getCode() + ".proto.txt");
            }
            CpSolver solver = new CpSolver();
            if (req.isEnumerateAllSolution()) {
                solver.getParameters().setEnumerateAllSolutions(true);
            }
            // 可按需设置更多参数
            // if (config != null) { solver.getParameters().setLogSearchProgress(true); }
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
            // if(cb.getAllSolutions().isEmpty()){
            // return Result.noSolution();
            // }

            // 执行后处理
            List<ModuleInst> solutions = cb.getAllSolutions();
            solutions = executePostProcess(module, solutions);

            return Result.success(solutions);
        } catch (Exception ex) {
            log.error("Failed to infer paras", ex);
            return Result.failed("exception: " + ex.getMessage());
        }
    }

    static class ModuleInstSolutionCallBack extends CpSolverSolutionCallback {

        private final Module module;

        private final List<Var<?>> vars;

        private final List<ModuleInst> allSolutions = new ArrayList<>();

        // 第几个解
        private int solutionIndex = 0;

        // 其他变量映射
        private Map<String, OtherVar> otherVarMap;

        // 常量定义
        private static final String OTHER_VARIABLES_VALUE_KEY = "OTHER_VARIABLES_VALUE";

        private static final String OTHER_VARIABLES_MEMO_KEY = "OTHER_VARIABLES_MEMO";

        public ModuleInstSolutionCallBack(Module module, List<Var<?>> vars) {
            this.vars = vars != null ? vars : Collections.emptyList();
            this.module = module;
        }

        public ModuleInstSolutionCallBack(Module module, List<Var<?>> vars, Map<String, OtherVar> otherVarMap) {
            this.vars = vars != null ? vars : Collections.emptyList();
            this.module = module;
            this.otherVarMap = otherVarMap;
        }

        @Override
        public void onSolutionCallback() {
            solutionIndex++;
            // 创建ModuleInst实例，instanceId从0开始
            ModuleInst moduleInst = createModuleInst(module, 0);
            for (Var<?> v : vars) {
                // 打印var.getVarString
                log.info("-------------varInfos-solutionIndex:{}----------- \n {}", solutionIndex,
                        v.getVarString(this));
                // 如果不是debug模式，则不打印Var的值
                if (v instanceof ParaVar) {
                    ParaVar pv = (ParaVar) v;
                    ParaInst pi = new ParaInst();
                    pi.setCode(pv.getCode());
                    // 从模块中获取对应的Para模型，设置shortCode
                    pi.setShortCode(pv.getBase().getShortCode());

                    // value: read IntVar domain value
                    int value = (int) value((IntVar) pv.value);
                    pi.setValue(String.valueOf(value));
                    // options: selected option codes
                    List<String> options = new ArrayList<>();
                    pv.optionSelectVars.forEach((codeId, optionVar) -> {
                        long sel = value(optionVar.getIsSelectedVar());
                        if (sel == 1L) {
                            options.add(optionVar.getCode());
                        }
                    });
                    pi.setOptions(options);
                    pi.setHidden((int) value((IntVar) pv.isHidden) == 1);
                    moduleInst.addParaInst(pi);
                } else if (v instanceof PartVar) {
                    PartVar partVar = (PartVar) v;
                    PartInst inst = new PartInst();
                    inst.setCode(partVar.getCode());
                    // 从模块中获取对应的Part模型，设置shortCode
                    inst.setShortCode(partVar.getBase().getShortCode());
                    inst.setQuantity((int) value((IntVar) partVar.qty));
                    moduleInst.addPartInst(inst);
                }
            }

            // 如果isLogVariables为true，则增加根据otherVarMap值的获取,组织otherVarKeyMap
            if (otherVarMap != null && !otherVarMap.isEmpty()) {
                Map<String, Long> otherVarKeyMap = new HashMap<>();
                for (Map.Entry<String, OtherVar> entry : otherVarMap.entrySet()) {
                    OtherVar otherVar = entry.getValue();
                    long varValue = value(otherVar.getVar());
                    otherVarKeyMap.put(otherVar.getShortCode(), varValue);
                }
                moduleInst.getExtAttrs().put(OTHER_VARIABLES_VALUE_KEY, otherVarKeyMap);
                moduleInst.getExtAttrs().put(OTHER_VARIABLES_MEMO_KEY, otherVarMap);
            }

            allSolutions.add(moduleInst);
        }

        public List<ModuleInst> getAllSolutions() {
            return allSolutions;
        }
    }

    /**
     * 创建ModuleInst实例
     * 
     * @param module     模块对象
     * @param instanceId 实例ID，默认为0
     * @return ModuleInst实例
     */
    private static ModuleInst createModuleInst(Module module, int instanceId) {
        ModuleInst moduleInst = new ModuleInst();
        moduleInst.setId(module.getId());
        moduleInst.setCode(module.getCode());
        // 设置ModuleInst的shortCode
        moduleInst.setShortCode(module.getShortCode());
        moduleInst.setInstanceConfigId("0");
        moduleInst.setInstanceId(instanceId);
        moduleInst.setQuantity(1);
        moduleInst.setParas(new ArrayList<>());
        moduleInst.setParts(new ArrayList<>());
        return moduleInst;
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

        try {
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
        } catch (Exception e) {
            log.error("Failed to register extensible process: {}", eProcess.getProcessName(), e);
            return Result.failed("Exception: " + e.getMessage());
        }
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

        try {
            eProcess.destroy();
            extensibleProcesses.remove(eProcess);
            log.info("Unregistered extensible process: {}", eProcess.getProcessName());
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to unregister extensible process: {}", eProcess.getProcessName(), e);
            return Result.failed("Exception: " + e.getMessage());
        }
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
                try {
                    InferParasPostProcess postProcess = (InferParasPostProcess) process;
                    Result<List<ModuleInst>> processResult = postProcess.postProcess(module, result);

                    if (processResult.getCode() == Result.SUCCESS && processResult.getData() != null) {
                        result = processResult.getData();
                        log.debug("Applied post process: {} successfully", process.getProcessName());
                    } else {
                        log.warn("Post process failed: {}, message: {}",
                                process.getProcessName(), processResult.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Exception in post process: {}", process.getProcessName(), e);
                }
            }
        }

        return result;
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
        } catch (Exception e) {
            return "{\"error\": \"Serialization failed: " + e.getMessage() + "\"}";
        }
    }
}