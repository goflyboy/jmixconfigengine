package com.jmix.configengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 规则定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Rule extends Extensible {
    /**
     * 规则编码
     */
    private String code;
    
    /**
     * 规则名称
     */
    private String name;
    
    /**
     * 可编程对象类型
     */
    private String progObjType;
    
    /**
     * 可编程对象编码
     */
    private String progObjCode;
    
    /**
     * 可编程对象属性
     */
    private String progObjField;
    
    /**
     * 规范化自然语言表示
     */
    private String normalNaturalCode;
    
    /**
     * 规则原始代码
     */
    private String rawCode;
    
    /**
     * 规则表达范式
     */
    private String ruleSchema;
} 