package com.jmix.executor.inf;

import lombok.Data;

/**
 * 约束配置类
 * 
 * @since 2025-9-21
 */
@Data
public class ConstraintConfig {
    private boolean isAttachedDebug;

    private String rootFilePath;

    private String logFilePath;

    // 是否输出ModelProto信息，方便定位
    private boolean isLogModelProto = false;

    // 是否记录所有的变量
    private boolean isLogVariables = true;

    // 加载模式，全量加载模式-1，差量加载模式-0
    private int loadType = 0;
}
