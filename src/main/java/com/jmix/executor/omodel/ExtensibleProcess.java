package com.jmix.executor.omodel;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 扩展性处理基类
 * 为ModuleConstraintExecutor提供扩展能力，支持自定义业务处理逻辑
 * 
 * @author jmix
 * @version 1.0.0
 */
@Slf4j
@Data
public abstract class ExtensibleProcess {

    /**
     * 扩展处理名称
     */
    private String processName;

    /**
     * 扩展处理版本
     */
    private String version = "1.0.0";

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 优先级，数值越小优先级越高
     */
    private int priority = 100;

    public ExtensibleProcess() {
        this.processName = this.getClass().getSimpleName();
    }

    public ExtensibleProcess(String processName) {
        this.processName = processName;
    }

    /**
     * 初始化扩展处理
     * 
     * @return 初始化结果
     */
    public Result<Void> init() {
        log.info("Initializing extensible process: {}", processName);
        return Result.success(null);
    }

    /**
     * 销毁扩展处理
     * 
     * @return 销毁结果
     */
    public Result<Void> destroy() {
        log.info("Destroying extensible process: {}", processName);
        return Result.success(null);
    }
}