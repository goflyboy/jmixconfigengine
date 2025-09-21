package com.jmix.executor;

import com.jmix.executor.impl.ModuleConstraintExecutorImpl;
import com.jmix.executor.inf.ConstraintConfig;
import com.jmix.executor.inf.ExtensibleProcess;
import com.jmix.executor.inf.InferParasReq;
import com.jmix.executor.inf.ModuleInst;
import com.jmix.executor.inf.Result;
import com.jmix.executor.model.Module;

import java.util.List;

/**
 * 模块约束执行器单例类
 * 提供全局访问点，外部只能通过此接口访问功能
 * 
 * @since 2025-9-21
 */
public interface ModuleConstraintExecutor {

    /**
     * 内部实现实例
     */
    public final ModuleConstraintExecutorImpl INST = new ModuleConstraintExecutorImpl();

    /**
     * 初始化执行器
     * 
     * @param config 约束配置
     * @return 初始化结果
     */
    public Result<Void> init(final ConstraintConfig config);

    /**
     * 销毁执行器
     * 
     * @return 销毁结果
     */
    public Result<Void> fini();

    /**
     * 添加模块
     * 
     * @param rootModuleId 根模块ID
     * @param modules      模块数组
     * @return 添加结果
     */
    public Result<Void> addModule(Long rootModuleId, Module... modules);

    /**
     * 移除模块
     * 
     * @param moduleId 模块ID
     * @return 移除结果
     */
    public Result<Void> removeModule(Long moduleId);

    /**
     * 推理参数
     * 
     * @param req 推理请求
     * @return 推理结果
     */
    public Result<List<ModuleInst>> inferParas(InferParasReq req);

    /**
     * 注册扩展处理器
     * 
     * @param eProcess 扩展处理器
     * @return 注册结果
     */
    public Result<Void> registerExtensible(ExtensibleProcess eProcess);

    /**
     * 注销扩展处理器
     * 
     * @param eProcess 扩展处理器
     * @return 注销结果
     */
    public Result<Void> unregisterExtensible(ExtensibleProcess eProcess);
}