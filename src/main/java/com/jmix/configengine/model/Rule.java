package com.jmix.configengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.jmix.configengine.model.schema.RuleSchema;
import com.jmix.configengine.model.schema.CodeRuleSchema;
import com.jmix.configengine.model.schema.CompatiableRuleSchema;
import com.jmix.configengine.model.schema.SelectRuleSchema;
import com.jmix.configengine.model.schema.RefProgObjSchema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.List;
import java.util.ArrayList;

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
    
    /**
     * 获取规则左侧引用的编程对象列表
     * @return 左侧引用的编程对象列表
     */
    @JsonIgnore
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        if (rawCode == null) {
            return new ArrayList<>();
        }
        
        if (rawCode instanceof CodeRuleSchema) {
            CodeRuleSchema codeRuleSchema = (CodeRuleSchema) rawCode;
            return codeRuleSchema.getLeftRefProgObjs() != null ? codeRuleSchema.getLeftRefProgObjs() : new ArrayList<>();
        } else if (rawCode instanceof CompatiableRuleSchema) {
            CompatiableRuleSchema compatiableRuleSchema = (CompatiableRuleSchema) rawCode;
            if (compatiableRuleSchema.getLeftExpr() != null && compatiableRuleSchema.getLeftExpr().getRefProgObjs() != null) {
                return compatiableRuleSchema.getLeftExpr().getRefProgObjs();
            }
        } else if (rawCode instanceof SelectRuleSchema) {
            SelectRuleSchema selectRuleSchema = (SelectRuleSchema) rawCode;
            if (selectRuleSchema.getLeftExpr() != null && selectRuleSchema.getLeftExpr().getRefProgObjs() != null) {
                return selectRuleSchema.getLeftExpr().getRefProgObjs();
            }
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 获取规则右侧引用的编程对象列表
     * @return 右侧引用的编程对象列表
     */
    @JsonIgnore
    public List<RefProgObjSchema> getToRightProgObjs() {
        if (rawCode == null) {
            return new ArrayList<>();
        }
        
        if (rawCode instanceof CodeRuleSchema) {
            CodeRuleSchema codeRuleSchema = (CodeRuleSchema) rawCode;
            return codeRuleSchema.getRightRefProgObjs() != null ? codeRuleSchema.getRightRefProgObjs() : new ArrayList<>();
        } else if (rawCode instanceof CompatiableRuleSchema) {
            CompatiableRuleSchema compatiableRuleSchema = (CompatiableRuleSchema) rawCode;
            if (compatiableRuleSchema.getRightExpr() != null && compatiableRuleSchema.getRightExpr().getRefProgObjs() != null) {
                return compatiableRuleSchema.getRightExpr().getRefProgObjs();
            }
        }
        
        return new ArrayList<>();
    }
    
} 