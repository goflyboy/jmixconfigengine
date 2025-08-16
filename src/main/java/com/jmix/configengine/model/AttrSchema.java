package com.jmix.configengine.model;

import lombok.Data;

/**
 * 属性Schema
 */
@Data
public class AttrSchema {
    /**
     * 属性编码
     */
    private String code;
    
    /**
     * 属性名称
     */
    private String name;
    
    /**
     * 属性类型
     */
    private String type;
    
    /**
     * 属性描述
     */
    private String description;
} 