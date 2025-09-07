package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.ModuleConstraintExecutor;
import com.jmix.configengine.executor.ModuleConstraintExecutorImpl;
import com.jmix.configengine.model.Module;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * DC公司的模块约束执行器实现
 * 包装了原有的ModuleConstraintExecutorImpl，提供DC特有的功能
 */
@Slf4j
public class DCModuleConstraintExecutorImpl implements DCModuleConstraintExecutor {
    
    private final ModuleConstraintExecutorImpl wrappedExecutor;
    private final DCModuleAdapter moduleAdapter;
    private final DCParaInstAdapter paraInstAdapter;
    private final DCPartInstAdapter partInstAdapter;
    
    public DCModuleConstraintExecutorImpl() {
        this.wrappedExecutor = new ModuleConstraintExecutorImpl();
        this.moduleAdapter = new DCModuleAdapter();
        this.paraInstAdapter = new DCParaInstAdapter();
        this.partInstAdapter = new DCPartInstAdapter();
    }
    
    @Override
    public DCResult<Void> init(ModuleConstraintExecutor.ConstraintConfig config) {
        log.info("Initializing DC Module Constraint Executor");
        ModuleConstraintExecutor.Result<Void> result = wrappedExecutor.init(config);
        DCResult<Void> dcResult = new DCResult<>(result);
        dcResult.setDcResultCode("DC_INIT_SUCCESS");
        dcResult.setExtAttr("executorType", "DCModuleConstraintExecutor");
        return dcResult;
    }
    
    @Override
    public DCResult<Void> fini() {
        log.info("Finalizing DC Module Constraint Executor");
        ModuleConstraintExecutor.Result<Void> result = wrappedExecutor.fini();
        DCResult<Void> dcResult = new DCResult<>(result);
        dcResult.setDcResultCode("DC_FINI_SUCCESS");
        dcResult.setExtAttr("executorType", "DCModuleConstraintExecutor");
        return dcResult;
    }
    
    @Override
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
        ModuleConstraintExecutor.Result<Void> result = wrappedExecutor.addModule(rootModuleId, modules);
        DCResult<Void> dcResult = new DCResult<>(result);
        dcResult.setDcResultCode("DC_ADD_MODULE_SUCCESS");
        dcResult.setExtAttr("moduleCount", String.valueOf(dcModules.length));
        dcResult.setExtAttr("rootModuleId", String.valueOf(rootModuleId));
        return dcResult;
    }
    
    @Override
    public DCResult<Void> removeModule(Long moduleId) {
        log.info("Removing module: {}", moduleId);
        ModuleConstraintExecutor.Result<Void> result = wrappedExecutor.removeModule(moduleId);
        DCResult<Void> dcResult = new DCResult<>(result);
        dcResult.setDcResultCode("DC_REMOVE_MODULE_SUCCESS");
        dcResult.setExtAttr("moduleId", String.valueOf(moduleId));
        return dcResult;
    }
    
    @Override
    public DCResult<List<DCModuleInst>> inferDCParas(DCInferParasReq req) {
        if (req == null) {
            return DCResult.failed("DC request is null", "DC_INFER_FAILED");
        }
        
        // 将DC请求转换为标准请求
        ModuleConstraintExecutor.InferParasReq standardReq = convertToStandardReq(req);
        
        // 执行推理
        ModuleConstraintExecutor.Result<List<ModuleConstraintExecutor.ModuleInst>> result = 
                wrappedExecutor.inferParas(standardReq);
        
        if (result.code != ModuleConstraintExecutor.Result.SUCCESS) {
            return DCResult.failed("Inference failed: " + result.message, "DC_INFER_FAILED");
        }
        
        // 将结果转换为DC格式
        List<DCModuleInst> dcSolutions = new ArrayList<>();
        if (result.data != null) {
            for (ModuleConstraintExecutor.ModuleInst solution : result.data) {
                DCModuleInst dcSolution = convertToDCModuleInst(solution);
                dcSolutions.add(dcSolution);
            }
        }
        
        log.info("Inferred {} DC solutions", dcSolutions.size());
        DCResult<List<DCModuleInst>> dcResult = DCResult.success(dcSolutions, "DC_INFER_SUCCESS");
        dcResult.setExtAttr("solutionCount", String.valueOf(dcSolutions.size()));
        dcResult.setExtAttr("moduleCode", req.moduleCode);
        return dcResult;
    }
    
    @Override
    public DCResult<Void> registerExtensible(com.jmix.configengine.ExtensibleProcess eProcess) {
        log.info("Registering extensible process: {}", eProcess.getProcessName());
        ModuleConstraintExecutor.Result<Void> result = wrappedExecutor.registerExtensible(eProcess);
        DCResult<Void> dcResult = new DCResult<>(result);
        dcResult.setDcResultCode("DC_REGISTER_EXTENSIBLE_SUCCESS");
        dcResult.setExtAttr("processName", eProcess.getProcessName());
        dcResult.setExtAttr("processPriority", String.valueOf(eProcess.getPriority()));
        return dcResult;
    }
    
    @Override
    public DCResult<Void> unregisterExtensible(com.jmix.configengine.ExtensibleProcess eProcess) {
        log.info("Unregistering extensible process: {}", eProcess.getProcessName());
        ModuleConstraintExecutor.Result<Void> result = wrappedExecutor.unregisterExtensible(eProcess);
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
        
        log.debug("Converted DCModule to Module: {} -> {}", dcModule.getDccode(), module.getCode());
        return module;
    }
    
    /**
     * 将DC请求转换为标准请求
     */
    private ModuleConstraintExecutor.InferParasReq convertToStandardReq(DCInferParasReq dcReq) {
        ModuleConstraintExecutor.InferParasReq standardReq = new ModuleConstraintExecutor.InferParasReq();
        standardReq.moduleId = dcReq.moduleId;
        standardReq.moduleCode = dcReq.moduleCode;
        standardReq.enumerateAllSolution = dcReq.enumerateAllSolution;
        
        // 转换主部件实例
        if (dcReq.mainPartInst != null) {
            standardReq.mainPartInst = convertToStandardPartInst(dcReq.mainPartInst);
        }
        
        // 转换预参数实例
        if (dcReq.preParaInsts != null) {
            standardReq.preParaInsts = new ArrayList<>();
            for (DCParaInst dcParaInst : dcReq.preParaInsts) {
                standardReq.preParaInsts.add(convertToStandardParaInst(dcParaInst));
            }
        }
        
        // 转换预部件实例
        if (dcReq.prePartInsts != null) {
            standardReq.prePartInsts = new ArrayList<>();
            for (DCPartInst dcPartInst : dcReq.prePartInsts) {
                standardReq.prePartInsts.add(convertToStandardPartInst(dcPartInst));
            }
        }
        
        return standardReq;
    }
    
    /**
     * 将DCPartInst转换为标准PartInst
     */
    private ModuleConstraintExecutor.PartInst convertToStandardPartInst(DCPartInst dcPartInst) {
        if (dcPartInst == null) {
            return null;
        }
        
        ModuleConstraintExecutor.PartInst partInst = new ModuleConstraintExecutor.PartInst();
        partInst.setCode(dcPartInst.getCode());
        partInst.setQuantity(dcPartInst.getQuantity());
        partInst.setSelectAttrValue(dcPartInst.getSelectAttrValue());
        partInst.setHidden(dcPartInst.isHidden());
        
        return partInst;
    }
    
    /**
     * 将DCParaInst转换为标准ParaInst
     */
    private ModuleConstraintExecutor.ParaInst convertToStandardParaInst(DCParaInst dcParaInst) {
        if (dcParaInst == null) {
            return null;
        }
        
        ModuleConstraintExecutor.ParaInst paraInst = new ModuleConstraintExecutor.ParaInst();
        paraInst.setCode(dcParaInst.getCode());
        paraInst.setValue(dcParaInst.getValue());
        paraInst.setOptions(dcParaInst.getOptions());
        paraInst.setHidden(dcParaInst.isHidden());
        
        return paraInst;
    }
    
    /**
     * 将标准ModuleInst转换为DCModuleInst
     */
    private DCModuleInst convertToDCModuleInst(ModuleConstraintExecutor.ModuleInst solution) {
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
            for (ModuleConstraintExecutor.ParaInst paraInst : solution.getParas()) {
                DCParaInst dcParaInst = paraInstAdapter.adapt(paraInst);
                dcParas.add(dcParaInst);
            }
            // 注意：这里需要将DCParaInst转换回标准ParaInst
            List<ModuleConstraintExecutor.ParaInst> standardParas = new ArrayList<>();
            for (DCParaInst dcParaInst : dcParas) {
                standardParas.add(convertToStandardParaInst(dcParaInst));
            }
            dcSolution.setParas(standardParas);
        }
        
        // 转换部件实例
        if (solution.getParts() != null) {
            List<DCPartInst> dcParts = new ArrayList<>();
            for (ModuleConstraintExecutor.PartInst partInst : solution.getParts()) {
                DCPartInst dcPartInst = partInstAdapter.adapt(partInst);
                dcParts.add(dcPartInst);
            }
            // 注意：这里需要将DCPartInst转换回标准PartInst
            List<ModuleConstraintExecutor.PartInst> standardParts = new ArrayList<>();
            for (DCPartInst dcPartInst : dcParts) {
                standardParts.add(convertToStandardPartInst(dcPartInst));
            }
            dcSolution.setParts(standardParts);
        }
        
        log.debug("Converted ModuleInst to DCModuleInst: {} -> {}", solution.getCode(), dcSolution.getDccode());
        return dcSolution;
    }
}
