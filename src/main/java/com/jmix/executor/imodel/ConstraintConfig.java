package com.jmix.executor.imodel;

import lombok.Data;

/**
 * 约束配置类
 * 包含约束求解器的配置信息
 * 
 * @since 2025-09-22
 */
@Data
public class ConstraintConfig {
    /**
     * 是否附加调试信息
     */
    private boolean isAttachedDebug;

    /**
     * 根文件路径
     */
    private String rootFilePath;

    /**
     * 日志文件路径
     */
    private String logFilePath;

    /**
     * 是否输出ModelProto信息，方便定位
     */
    private boolean isLogModelProto = false;

    /**
     * 是否记录所有的变量
     */
    private boolean isLogVariables = true;

    /**
     * 加载模式，全量加载模式-1，差量加载模式-0
     */
    private int loadType = 1;
}
