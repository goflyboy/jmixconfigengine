package com.jmix.configengine.inf;

/**
 * 约束配置类
 */
public class ConstraintConfig {
    public boolean isAttachedDebug;
    public String rootFilePath;
    public String logFilePath;
    public boolean isLogModelProto = false; // 是否输出ModelProto信息，方便定位
    public boolean isLogVariables = false; // 是否记录所有的变量
}
