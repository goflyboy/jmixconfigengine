package com.jmix.tool.extensibleDemo;

import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.omodel.ExtensibleProcess;

import lombok.Data;

import java.util.List;

/**
 * DC公司的模块约束执行器接口
 * 包装了原有的ModuleConstraintExecutor，提供DC特有的功能
 * 使用单例模式，外部只能访问接口，不能直接访问实现类
 */
public interface DCModuleConstraintExecutor {

    /**
     * 单例实例
     */
    DCModuleConstraintExecutor INST = new DCModuleConstraintExecutorImpl();

    /**
     * 初始化执行器
     * 
     * @param config 约束配置
     * @return 初始化结果
     */
    DCResult<Void> init(ConstraintConfig config);

    /**
     * 销毁执行器
     * 
     * @return 销毁结果
     */
    DCResult<Void> fini();

    /**
     * 添加DC模块
     * 
     * @param rootModuleId 根模块ID
     * @param dcModules    DC模块数组
     * @return 添加结果
     */
    DCResult<Void> addDCModule(Long rootModuleId, DCModule... dcModules);

    /**
     * 移除模块
     * 
     * @param moduleId 模块ID
     * @return 移除结果
     */
    DCResult<Void> removeModule(Long moduleId);

    /**
     * 推理DC参数
     * 
     * @param req 推理请求
     * @return 推理结果
     */
    DCResult<List<DCModuleInst>> inferDCParas(DCInferParasReq req);

    /**
     * 注册扩展处理器
     * 
     * @param eProcess 扩展处理器
     * @return 注册结果
     */
    DCResult<Void> registerExtensible(ExtensibleProcess eProcess);

    /**
     * 注销扩展处理器
     * 
     * @param eProcess 扩展处理器
     * @return 注销结果
     */
    DCResult<Void> unregisterExtensible(ExtensibleProcess eProcess);

    /**
     * DC推理请求类
     */
    @Data
    public static class DCInferParasReq {

        private Long moduleId;

        private String moduleCode;

        private DCPartInst mainPartInst;

        private List<DCParaInst> preParaInsts;

        private List<DCPartInst> prePartInsts;

        private boolean enumerateAllSolution;
    }
}
