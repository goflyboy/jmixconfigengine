package com.jmix.configengine;

import com.jmix.configengine.model.Module;

import java.util.List;

/**
 * 参数推理后处理接口
 * 在ModuleConstraintExecutor.inferParas执行完成后，对结果进行后处理
 * 
 * @author jmix
 * @version 1.0.0
 */
public interface InferParasPostProcess {
    
    /**
     * 默认操作类型
     */
    String OPERATION_INFER_PARAS_POST = "inferParasPost";
    
    /**
     * 对推理结果进行后处理
     * 
     * @param module 模块定义
     * @param solutions 推理结果列表
     * @return 处理结果
     */
    ModuleConstraintExecutor.Result<List<ModuleConstraintExecutor.ModuleInst>> postProcess(Module module, List<ModuleConstraintExecutor.ModuleInst> solutions);
}
