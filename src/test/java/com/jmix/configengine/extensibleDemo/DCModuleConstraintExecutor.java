package com.jmix.configengine.extensibleDemo;

import com.jmix.configengine.ModuleConstraintExecutor;
import com.jmix.configengine.inf.ConstraintConfig;

import java.util.List;

/**
 * DC公司的模块约束执行器接口
 * 包装了原有的ModuleConstraintExecutor，提供DC特有的功能
 */
public interface DCModuleConstraintExecutor {
    
    /**
     * 初始化执行器
     * @param config 约束配置
     * @return 初始化结果
     */
    DCResult<Void> init(ConstraintConfig config);
    
    /**
     * 销毁执行器
     * @return 销毁结果
     */
    DCResult<Void> fini();
    
    /**
     * 添加DC模块
     * @param rootModuleId 根模块ID
     * @param dcModules DC模块数组
     * @return 添加结果
     */
    DCResult<Void> addDCModule(Long rootModuleId, DCModule... dcModules);
    
    /**
     * 移除模块
     * @param moduleId 模块ID
     * @return 移除结果
     */
    DCResult<Void> removeModule(Long moduleId);
    
    /**
     * 推理DC参数
     * @param req 推理请求
     * @return 推理结果
     */
    DCResult<List<DCModuleInst>> inferDCParas(DCInferParasReq req);
    
    /**
     * 注册扩展处理器
     * @param eProcess 扩展处理器
     * @return 注册结果
     */
    DCResult<Void> registerExtensible(com.jmix.configengine.inf.ExtensibleProcess eProcess);
    
    /**
     * 注销扩展处理器
     * @param eProcess 扩展处理器
     * @return 注销结果
     */
    DCResult<Void> unregisterExtensible(com.jmix.configengine.inf.ExtensibleProcess eProcess);
    
    /**
     * DC推理请求类
     */
    class DCInferParasReq {
        public Long moduleId;
        public String moduleCode;
        public DCPartInst mainPartInst;
        public List<DCParaInst> preParaInsts;
        public List<DCPartInst> prePartInsts;
        public boolean enumerateAllSolution;
    }
}
