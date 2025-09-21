package com.jmix.configengine.extensibleDemo;

import java.util.List;

import com.jmix.executor.inf.ConstraintConfig;
import com.jmix.executor.inf.ExtensibleProcess;

/**
 * DC公司的模块约束执行器接口
 * 包装了原有的ModuleConstraintExecutor，提供DC特有的功能
 * 使用单例模式，外部只能访问接口，不能直接访问实现类
 */
public class DCModuleConstraintExecutor {

    /**
     * 单例实例
     */
    public static final DCModuleConstraintExecutor INST = new DCModuleConstraintExecutor();

    /**
     * 内部实现实例
     */
    private final DCModuleConstraintExecutorImpl impl = new DCModuleConstraintExecutorImpl();

    /**
     * 私有构造函数，确保单例
     */
    private DCModuleConstraintExecutor() {
    }

    /**
     * 初始化执行器
     * 
     * @param config 约束配置
     * @return 初始化结果
     */
    public DCResult<Void> init(ConstraintConfig config) {
        return impl.init(config);
    }

    /**
     * 销毁执行器
     * 
     * @return 销毁结果
     */
    public DCResult<Void> fini() {
        return impl.fini();
    }

    /**
     * 添加DC模块
     * 
     * @param rootModuleId 根模块ID
     * @param dcModules    DC模块数组
     * @return 添加结果
     */
    public DCResult<Void> addDCModule(Long rootModuleId, DCModule... dcModules) {
        return impl.addDCModule(rootModuleId, dcModules);
    }

    /**
     * 移除模块
     * 
     * @param moduleId 模块ID
     * @return 移除结果
     */
    public DCResult<Void> removeModule(Long moduleId) {
        return impl.removeModule(moduleId);
    }

    /**
     * 推理DC参数
     * 
     * @param req 推理请求
     * @return 推理结果
     */
    public DCResult<List<DCModuleInst>> inferDCParas(DCInferParasReq req) {
        return impl.inferDCParas(req);
    }

    /**
     * 注册扩展处理器
     * 
     * @param eProcess 扩展处理器
     * @return 注册结果
     */
    public DCResult<Void> registerExtensible(ExtensibleProcess eProcess) {
        return impl.registerExtensible(eProcess);
    }

    /**
     * 注销扩展处理器
     * 
     * @param eProcess 扩展处理器
     * @return 注销结果
     */
    public DCResult<Void> unregisterExtensible(ExtensibleProcess eProcess) {
        return impl.unregisterExtensible(eProcess);
    }

    /**
     * DC推理请求类
     */
    public static class DCInferParasReq {
        public Long moduleId;
        public String moduleCode;
        public DCPartInst mainPartInst;
        public List<DCParaInst> preParaInsts;
        public List<DCPartInst> prePartInsts;
        public boolean enumerateAllSolution;
    }
}
