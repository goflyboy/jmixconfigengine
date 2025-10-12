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
     * 差量加载
     */
    public static final int LOAD_TYPE_INCREMENTAL = 0;

    /**
     * 全量加载
     */
    public static final int LOAD_TYPE_FULL = 1;

    /**
     * 是否Attach方式调试，true-工程内class加载，可直接调试算， false-jar包方式加载，不可以调试
     */
    private boolean isAttachedDebug = false;

    /**
     * 根文件路径
     */
    private String rootFilePath = ".";

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
     * 加载模式：全量(1) 或 差量(0)
     */
    private int loadType = LOAD_TYPE_FULL;

    /**
     * 是否通过增加松弛变量来调试冲突规则
     */
    private boolean debugByRelaxationVar = false;
}
