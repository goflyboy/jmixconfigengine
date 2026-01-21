package com.jmix.tool.extensibleDemo;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.ConstraintConfig;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.omodel.ExtensibleProcess;
import com.jmix.executor.omodel.InferParasReq;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.ParaInst;
import com.jmix.executor.omodel.PartInst;
import com.jmix.executor.omodel.Result;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * DC公司的模块约束执行器实现
 * 包装了原有的ModuleConstraintExecutorImpl，提供DC特有的功能
 * 内部实现类，外部不应直接访问
 */
@Slf4j
class DCModuleConstraintExecutorImpl implements DCModuleConstraintExecutor {

    // 使用单例访问，不需要包装实例
    private final DCParaInstAdapter paraInstAdapter;

    private final DCPartInstAdapter partInstAdapter;

    public DCModuleConstraintExecutorImpl() {
        this.paraInstAdapter = new DCParaInstAdapter();
        this.partInstAdapter = new DCPartInstAdapter();
    }

    public DCResult<Void> init(ConstraintConfig config) {
        log.info("Initializing DC Module Constraint Executor");
        Result<Void> result = ModuleConstraintExecutor.INST.init(config);
        DCResult<Void> dcResult = new DCResult<>(result);
        dcResult.setDcResultCode("DC_INIT_SUCCESS");
        dcResult.setExtAttr("executorType", "DCModuleConstraintExecutor");
        return dcResult;
    }

    public DCResult<Void> fini() {
        log.info("Finalizing DC Module Constraint Executor");
        Result<Void> result = ModuleConstraintExecutor.INST.fini();
        DCResult<Void> dcResult = new DCResult<>(result);
        dcResult.setDcResultCode("DC_FINI_SUCCESS");
        dcResult.setExtAttr("executorType", "DCModuleConstraintExecutor");
        return dcResult;
    }

    public DCResult<Void> addDCModule(Long rootModuleId, DCModule... dcModules) {
        if (dcModules == null) {
            return DCResult.failed("DC modules is null", "DC_ADD_MODULE_FAILED");
        }

        // 将DCModule转换为Module
        Module[] modules = new Module[dcModules.length];
        for (int i = 0; i < dcModules.length; i++) {
            modules[i] = convertFromDCModule(dcModules[i]);
        }

        log.info("Adding {} DC modules", dcModules.length);
        Result<Void> result = ModuleConstraintExecutor.INST.addModule(rootModuleId, modules);
        DCResult<Void> dcResult = new DCResult<>(result);
        dcResult.setDcResultCode("DC_ADD_MODULE_SUCCESS");
        dcResult.setExtAttr("moduleCount", String.valueOf(dcModules.length));
        dcResult.setExtAttr("rootModuleId", String.valueOf(rootModuleId));
        return dcResult;
    }

    public DCResult<Void> removeModule(Long moduleId) {
        log.info("Removing module: {}", moduleId);
        Result<Void> result = ModuleConstraintExecutor.INST.removeModule(moduleId);
        DCResult<Void> dcResult = new DCResult<>(result);
        dcResult.setDcResultCode("DC_REMOVE_MODULE_SUCCESS");
        dcResult.setExtAttr("moduleId", String.valueOf(moduleId));
        return dcResult;
    }

    public DCResult<List<DCModuleInst>> inferDCParas(DCModuleConstraintExecutor.DCInferParasReq req) {
        if (req == null) {
            return DCResult.failed("DC request is null", "DC_INFER_FAILED");
        }

        // 将DC请求转换为标准请求
        InferParasReq standardReq = convertToStandardReq(req);

        // 执行推理
        Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(standardReq);

        if (result.getCode() != Result.SUCCESS) {
            return DCResult.failed("Inference failed: " + result.getMessage(), "DC_INFER_FAILED");
        }

        // 将结果转换为DC格式
        List<DCModuleInst> dcSolutions = new ArrayList<>();
        if (result.getData() != null) {
            for (ModuleInst solution : result.getData()) {
                DCModuleInst dcSolution = convertToDCModuleInst(solution);
                dcSolutions.add(dcSolution);
            }
        }

        log.info("Inferred {} DC solutions", dcSolutions.size());
        DCResult<List<DCModuleInst>> dcResult = DCResult.success(dcSolutions, "DC_INFER_SUCCESS");
        dcResult.setExtAttr("solutionCount", String.valueOf(dcSolutions.size()));
        dcResult.setExtAttr("moduleCode", req.getModuleCode());
        return dcResult;
    }

    public DCResult<Void> registerExtensible(ExtensibleProcess eProcess) {
        log.info("Registering extensible process: {}", eProcess.getProcessName());
        Result<Void> result = ModuleConstraintExecutor.INST.registerExtensible(eProcess);
        DCResult<Void> dcResult = new DCResult<>(result);
        dcResult.setDcResultCode("DC_REGISTER_EXTENSIBLE_SUCCESS");
        dcResult.setExtAttr("processName", eProcess.getProcessName());
        dcResult.setExtAttr("processPriority", String.valueOf(eProcess.getPriority()));
        return dcResult;
    }

    public DCResult<Void> unregisterExtensible(ExtensibleProcess eProcess) {
        log.info("Unregistering extensible process: {}", eProcess.getProcessName());
        Result<Void> result = ModuleConstraintExecutor.INST.unregisterExtensible(eProcess);
        DCResult<Void> dcResult = new DCResult<>(result);
        dcResult.setDcResultCode("DC_UNREGISTER_EXTENSIBLE_SUCCESS");
        dcResult.setExtAttr("processName", eProcess.getProcessName());
        return dcResult;
    }

    /**
     * 将DCModule转换为Module
     */
    private Module convertFromDCModule(DCModule dcModule) {
        if (dcModule == null) {
            return null;
        }

        Module module = new Module();
        module.setId(dcModule.getId());
        module.setCode(dcModule.getDccode() != null ? dcModule.getDccode() : dcModule.getCode());
        module.setVersion(dcModule.getVersion());
        module.setPackageName(dcModule.getPackageName());
        module.setType(dcModule.getType());
        module.setParas(dcModule.getParas());
        module.setParts(dcModule.getParts());
        module.setRules(dcModule.getRules());
        module.setAlg(dcModule.getAlg());
        module.init();

        log.info("Converted DCModule to Module: {} -> {}", dcModule.getDccode(), module.getCode());
        return module;
    }

    /**
     * 将DC请求转换为标准请求
     */
    private InferParasReq convertToStandardReq(DCModuleConstraintExecutor.DCInferParasReq dcReq) {
        InferParasReq standardReq = new InferParasReq();
        standardReq.setModuleId(dcReq.getModuleId());
        standardReq.setModuleCode(dcReq.getModuleCode());
        standardReq.setEnumerateAllSolution(dcReq.isEnumerateAllSolution());

        // 转换主部件实例
        if (dcReq.getMainPartInst() != null) {
            standardReq.setMainPartInst(convertToStandardPartInst(dcReq.getMainPartInst()));
        }

        // 转换预参数实例
        if (dcReq.getPreParaInsts() != null) {
            List<ParaInst> preParaInsts = new ArrayList<>();
            for (DCParaInst dcParaInst : dcReq.getPreParaInsts()) {
                preParaInsts.add(convertToStandardParaInst(dcParaInst));
            }
            standardReq.setPreParaInsts(preParaInsts);
        }

        // 转换预部件实例
        if (dcReq.getPrePartInsts() != null) {
            List<PartInst> prePartInsts = new ArrayList<>();
            for (DCPartInst dcPartInst : dcReq.getPrePartInsts()) {
                prePartInsts.add(convertToStandardPartInst(dcPartInst));
            }
            standardReq.setPrePartInsts(prePartInsts);
        }

        return standardReq;
    }

    /**
     * 将DCPartInst转换为标准PartInst
     */
    private PartInst convertToStandardPartInst(DCPartInst dcPartInst) {
        if (dcPartInst == null) {
            return null;
        }

        PartInst partInst = new PartInst();
        partInst.setCode(dcPartInst.getCode());
        partInst.setQuantity(dcPartInst.getQuantity());
        partInst.setSelectAttrValue(dcPartInst.getSelectAttrValue());
        partInst.setHidden(dcPartInst.isHidden());

        return partInst;
    }

    /**
     * 将DCParaInst转换为标准ParaInst
     */
    private ParaInst convertToStandardParaInst(DCParaInst dcParaInst) {
        if (dcParaInst == null) {
            return null;
        }

        ParaInst paraInst = new ParaInst();
        paraInst.setCode(dcParaInst.getCode());
        paraInst.setValue(dcParaInst.getValue());
        paraInst.setOptions(dcParaInst.getOptions());
        paraInst.setHidden(dcParaInst.isHidden());

        return paraInst;
    }

    /**
     * 将标准ModuleInst转换为DCModuleInst
     */
    private DCModuleInst convertToDCModuleInst(ModuleInst solution) {
        if (solution == null) {
            return null;
        }

        DCModuleInst dcSolution = new DCModuleInst(solution);

        // 设置DC特有字段
        dcSolution.setDccode(solution.getCode());
        dcSolution.setSeason("Spring"); // 默认季节

        // 转换参数实例
        if (solution.getParas() != null) {
            List<DCParaInst> dcParas = new ArrayList<>();
            for (ParaInst paraInst : solution.getParas()) {
                DCParaInst dcParaInst = paraInstAdapter.adapt(paraInst);
                dcParas.add(dcParaInst);
            }
            // 注意：这里需要将DCParaInst转换回标准ParaInst
            List<ParaInst> standardParas = new ArrayList<>();
            for (DCParaInst dcParaInst : dcParas) {
                standardParas.add(convertToStandardParaInst(dcParaInst));
            }
            dcSolution.setParas(standardParas);
        }

        // 转换部件实例
        if (solution.getParts() != null) {
            List<DCPartInst> dcParts = new ArrayList<>();
            for (PartInst partInst : solution.getParts()) {
                DCPartInst dcPartInst = partInstAdapter.adapt(partInst);
                dcParts.add(dcPartInst);
            }
            // 注意：这里需要将DCPartInst转换回标准PartInst
            List<PartInst> standardParts = new ArrayList<>();
            for (DCPartInst dcPartInst : dcParts) {
                standardParts.add(convertToStandardPartInst(dcPartInst));
            }
            dcSolution.setParts(standardParts);
        }

        log.info("Converted ModuleInst to DCModuleInst: {} -> {}", solution.getCode(), dcSolution.getDccode());
        return dcSolution;
    }
}
