package com.jmix.executor;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.impl.ModuleConstraintExecutorImpl;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.InferParasReq;
import com.jmix.executor.model.ModulePostCalcReq;
import com.jmix.executor.model.ModuleValidateReq;
import com.jmix.executor.model.ModuleValidateResp;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.model.Result;

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
    ModuleConstraintExecutorImpl INST = new ModuleConstraintExecutorImpl();

    /**
     * 初始化执行器
     * 
     * @param config 约束配置
     * @return 初始化结果
     */
    Result<Void> init(final ConstraintConfig config);

    /**
     * 销毁执行器
     * 
     * @return 销毁结果
     */
    Result<Void> fini();

    /**
     * 添加模块
     * 
     * @param rootModuleId 根模块ID
     * @param modules      模块数组
     * @return 添加结果
     */
    Result<Void> addModule(Long rootModuleId, Module... modules);

    /**
     * 移除模块
     * 
     * @param moduleId 模块ID
     * @return 移除结果
     */
    Result<Void> removeModule(Long moduleId);

    /**
     * 推理参数配置，根据部件的配置反推参数的配置 <br>
     * 如果req.enumerateAllSolution=false，仅返回一个可行解，<br>
     * 如果req.enumerateAllSolution=true，则返回所有的可选解 <br>
     * 
     * @param req 根据主部件配置(实例）及前置参数和部件的配置反推参数配置的值
     * @return 推理结果
     *         如果有解，则Result.SUCCESS，Result.data为详细的解数据<br>
     *         如果没有解，则返回Result.NO_SOLUTION <br>
     *         如果执行过程出错，则返回Result.FAILED <br>
     */
    Result<List<ModuleInst>> inferParas(InferParasReq req);

    boolean validate(ModuleInst moduleInst);

    Result<ModuleValidateResp> validate(ModuleValidateReq req);

    /**
     * 独立后置计算: 对已有解执行CalcStage.POST规则，写回派生参数
     *
     * @param req 后置计算请求
     * @return 写入派生参数后的解列表
     */
    Result<List<ModuleInst>> postCalculate(ModulePostCalcReq req);
}
