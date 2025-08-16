package com.jmix.configengine.schema;

import lombok.Data;

/**
 * 规则Schema基类
 */
@Data
public abstract class RuleSchema {
    /**
     * Schema类型
     */
    private String type;
    
    /**
     * Schema版本
     */
    private String version;
} 