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
    public String code;
    
    /**
     * 规则Schema类型全名
     * 例如：CDSL.V5.Struct.CompatiableRule
     */
    public String ruleSchemaTypeFullName;
    
    /**
     * 规则名称
     */
    public String name;
    
    /**
     * 自然语言描述
     */
    public String normalNaturalCode;
    
    /**
     * 左类型名称
     */
    public String leftTypeName;
    
    /**
     * 左变量信息
     */
    public VarInfo<? extends Extensible> left;

    /**
     * 左对象过滤后的列表-CodeId
     */
    public List<String> leftFilterCodes;//名字不太好，过滤后的结果 TODO
    
    /**
     * 右类型名称
     */
    public String rightTypeName;
    
    /**
     * 右变量信息
     */
    public VarInfo<? extends Extensible> right;

    
    /**
     * 左对象过滤后的列表-Code
     */
    public List<String> rightFilterCodes;
    
    public boolean isCompatibleRule(){
        return this.ruleSchemaTypeFullName.contains("CompatibleRule");
    }

    public boolean isCalculateRule(){
        return this.ruleSchemaTypeFullName.contains("CalculateRule");
    }
} 