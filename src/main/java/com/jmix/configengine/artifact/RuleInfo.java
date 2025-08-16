package com.jmix.configengine.artifact;

import lombok.Data;

/**
 * 规则信息
 */
@Data
public class RuleInfo {
    /**
     * 规则编码
     */
    private String code;
    
    /**
     * 规则Schema类型全名
     * 例如：CDSL.V5.Struct.CompatiableRule
     */
    private String ruleSchemaTypeFullName;
    
    /**
     * 规则名称
     */
    private String name;
    
    /**
     * 自然语言描述
     */
    private String normalNaturalCode;
    
    /**
     * 左类型名称
     */
    private String leftTypeName;
    
    /**
     * 左变量信息
     */
    private VarInfo left;
    
    /**
     * 右类型名称
     */
    private String rightTypeName;
    
    /**
     * 右变量信息
     */
    private VarInfo right;
} 