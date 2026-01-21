package com.jmix.executor.omodel;

import com.jmix.executor.bmodel.Module;

import java.util.List;

/**
 * 参数推理后处理接口
 * 在ModuleConstraintExecutor.inferParas执行完成后，对结果进行后处理
 * 
 * @since 2025-09-22
 */
public interface InferParasPostProcess {

    /**
     * 默认操作类型
     */
    String OPERATION_INFER_PARAS_POST = "inferParasPost";

    /**
     * 对推理结果进行后处理
     * 
     * @param module    模块定义
     * @param solutions 推理结果列表
     * @return 处理结果
     */
    Result<List<ModuleInst>> postProcess(Module module, List<ModuleInst> solutions);
}
