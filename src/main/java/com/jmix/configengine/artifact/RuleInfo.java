package com.jmix.configengine.artifact;

import java.util.List;

import com.jmix.configengine.model.Extensible;

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
    private VarInfo<? extends Extensible> left;

    /**
     * 左对象过滤后的列表-CodeId
     */
    private List<String> leftFilterCodes;
    
    /**
     * 右类型名称
     */
    private String rightTypeName;
    
    /**
     * 右变量信息
     */
    private VarInfo<? extends Extensible> right;

    
    /**
     * 左对象过滤后的列表-Code
     */
    private List<String> rightFilterCodes;
    
} 