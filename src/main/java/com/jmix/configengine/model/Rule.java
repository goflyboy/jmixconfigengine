package com.jmix.configengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.jmix.configengine.schema.RuleSchema;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

/**
 * 规则定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Rule.class, name = "Rule")
})
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
    private RuleSchema rawCode;
    
    /**
     * 规则表达范式类型全名
     * 例如：CDSL.V5.Struct.CompatiableRule
     */
    private String ruleSchemaTypeFullName;
} 