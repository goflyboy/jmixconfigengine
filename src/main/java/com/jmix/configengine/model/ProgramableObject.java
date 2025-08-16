package com.jmix.configengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 可编程对象基类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProgramableObject<T> extends Extensible {
    /**
     * 对象编码
     */
    private String code;
    
    /**
     * 父对象编码
     */
    private String fatherCode;
    
    /**
     * 默认值
     */
    private T defaultValue;
    
    /**
     * 描述信息
     */
    private String description;
    
    /**
     * 排序号
     */
    private Integer sortNo;
} 