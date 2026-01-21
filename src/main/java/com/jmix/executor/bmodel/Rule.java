package com.jmix.executor.bmodel;

import com.jmix.executor.bmodel.rule.RefProgObjSchema;
import com.jmix.executor.bmodel.rule.RuleSchema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则定义
 * 表示模块中的约束规则，支持多种规则类型
 * 
 * @since 2025-09-22
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
     * 父部件编码
     * 如果为空字符串，则规则添加到 Module 中；否则添加到对应的 PartCategory 中
     */
    private String fatherCode = "";

    /**
     * 获取规则左侧引用的编程对象列表
     * 
     * @return 左侧引用的编程对象列表
     */
    @JsonIgnore
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        if (rawCode == null) {
            return new ArrayList<>();
        }
        return rawCode.getFromLeftProgObjs();
    }

    /**
     * 获取规则右侧引用的编程对象列表
     * 
     * @return 右侧引用的编程对象列表
     */
    @JsonIgnore
    public List<RefProgObjSchema> getToRightProgObjs() {
        if (rawCode == null) {
            return new ArrayList<>();
        }
        return rawCode.getToRightProgObjs();
    }

}