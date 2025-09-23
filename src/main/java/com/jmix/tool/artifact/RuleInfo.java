package com.jmix.tool.artifact;

import com.jmix.executor.imodel.Extensible;
import com.jmix.executor.imodel.rule.RuleTypeConstants;

import lombok.Data;

import java.util.List;

/**
 * 规则信息
 * 包含规则的变量信息，用于代码生成
 * 
 * @since 2025-09-22
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
    private List<String> leftFilterCodes; // 名字不太好，过滤后的结果

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

    /**
     * 兼容性操作符
     * 例如：Requires、Incompatible、CoDependent
     */
    private String compatiableOperator;

    /**
     * 左变量名称
     * 例如：ColorVar、SizeVar等
     */
    private String leftVarName;

    /**
     * 右变量名称
     * 例如：ColorVar、SizeVar等
     */
    private String rightVarName;

    /**
     * 判断是否为兼容性规则
     * 
     * @return 如果是兼容性规则返回true，否则返回false
     */
    public boolean isCompatibleRule() {
        return RuleTypeConstants.isCompatiableRule(this.ruleSchemaTypeFullName);
    }

    /**
     * 判断是否为计算规则
     * 
     * @return 如果是计算规则返回true，否则返回false
     */
    public boolean isCalculateRule() {
        return RuleTypeConstants.isCalculateRule(this.ruleSchemaTypeFullName);
    }
}