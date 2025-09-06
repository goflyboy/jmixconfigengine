package com.jmix.configengine;

import lombok.extern.slf4j.Slf4j;

/**
 * 扩展性处理基类
 * 为ModuleConstraintExecutor提供扩展能力，支持自定义业务处理逻辑
 * 
 * @author jmix
 * @version 1.0.0
 */
@Slf4j
public abstract class ExtensibleProcess {
    
    /**
     * 扩展处理名称
     */
    protected String processName;
    
    /**
     * 扩展处理版本
     */
    protected String version = "1.0.0";
    
    /**
     * 是否启用
     */
    protected boolean enabled = true;
    
    /**
     * 优先级，数值越小优先级越高
     */
    protected int priority = 100;
    
    public ExtensibleProcess() {
        this.processName = this.getClass().getSimpleName();
    }
    
    public ExtensibleProcess(String processName) {
        this.processName = processName;
    }
    
    /**
     * 初始化扩展处理
     * @return 初始化结果
     */
    public ModuleConstraintExecutor.Result<Void> init() {
        log.info("Initializing extensible process: {}", processName);
        return ModuleConstraintExecutor.Result.success(null);
    }
    
    /**
     * 销毁扩展处理
     * @return 销毁结果
     */
    public ModuleConstraintExecutor.Result<Void> destroy() {
        log.info("Destroying extensible process: {}", processName);
        return ModuleConstraintExecutor.Result.success(null);
    }
    
    /**
     * 检查是否支持指定的操作
     * @param operation 操作类型
     * @return 是否支持
     */
    public abstract boolean supports(String operation);
    
    /**
     * 获取处理名称
     */
    public String getProcessName() {
        return processName;
    }
    
    /**
     * 设置处理名称
     */
    public void setProcessName(String processName) {
        this.processName = processName;
    }
    
    /**
     * 获取版本
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * 设置版本
     */
    public void setVersion(String version) {
        this.version = version;
    }
    
    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 设置是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 获取优先级
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * 设置优先级
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }
}