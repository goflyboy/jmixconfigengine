package com.jmix.configengine;

import com.jmix.configengine.model.Module;
import com.jmix.configengine.executor.ModuleConstraintExecutorImpl;
import com.jmix.configengine.inf.ConstraintConfig;
import com.jmix.configengine.inf.ExtensibleProcess;
import com.jmix.configengine.inf.InferParasReq;
import com.jmix.configengine.inf.ModuleInst;
import com.jmix.configengine.inf.Result;

import java.util.List;

/**
 * 模块约束执行器单例类
 * 提供全局访问点，外部只能通过此接口访问功能
 */
public class ModuleConstraintExecutor {
    
    /**
     * 单例实例
     */
    public static final ModuleConstraintExecutor INST = new ModuleConstraintExecutor();
    
    /**
     * 内部实现实例
     */
    private final ModuleConstraintExecutorImpl impl = new ModuleConstraintExecutorImpl();
    
    /**
     * 私有构造函数，防止外部实例化
     */
    private ModuleConstraintExecutor() {
    }
    
    /**
     * 初始化执行器
     * @param config 约束配置
     * @return 初始化结果
     */
    public Result<Void> init(ConstraintConfig config) {
        return impl.init(config);
    }
    
    /**
     * 销毁执行器
     * @return 销毁结果
     */
    public Result<Void> fini() {
        return impl.fini();
    }
    
    /**
     * 添加模块
     * @param rootModuleId 根模块ID
     * @param modules 模块数组
     * @return 添加结果
     */
    public Result<Void> addModule(Long rootModuleId, Module... modules) {
        return impl.addModule(rootModuleId, modules);
    }
    
    /**
     * 移除模块
     * @param moduleId 模块ID
     * @return 移除结果
     */
    public Result<Void> removeModule(Long moduleId) {
        return impl.removeModule(moduleId);
    }
    
    /**
     * 推理参数
     * @param req 推理请求
     * @return 推理结果
     */
    public Result<List<ModuleInst>> inferParas(InferParasReq req) {
        return impl.inferParas(req);
    }
    
    /**
     * 注册扩展处理器
     * @param eProcess 扩展处理器
     * @return 注册结果
     */
    public Result<Void> registerExtensible(ExtensibleProcess eProcess) {
        return impl.registerExtensible(eProcess);
    }
    
    /**
     * 注销扩展处理器
     * @param eProcess 扩展处理器
     * @return 注销结果
     */
    public Result<Void> unregisterExtensible(ExtensibleProcess eProcess) {
        return impl.unregisterExtensible(eProcess);
    }
} 