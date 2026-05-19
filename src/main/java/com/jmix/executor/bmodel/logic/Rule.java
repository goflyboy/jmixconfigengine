package com.jmix.executor.bmodel.logic;

import com.jmix.executor.bmodel.base.Extensible;

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
     * 组合规则父规则 code。为空表示独立规则或组合父规则。
     */
    private String parentRuleCode = "";

    /**
     * 结构化规则编译后的运行态 Schema。
     */
    private RuleSchema exeSchema;

    /**
     * 计算阶段
     * 用于指定规则在哪个计算阶段执行
     */
    private CalcStage calcStage = CalcStage.MID;

    /**
     * 左侧基数
     */
    private Cardinality leftCardinality = Cardinality.ONE;

    /**
     * 右侧基数
     */
    private Cardinality rightCardinality = Cardinality.ONE;

    /**
     * 作用范围
     */
    private EffectScope effectScope = EffectScope.SingleInst;

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

    @JsonIgnore
    public String getLeftCategoryCode() {
        return getCategoryCode(getFromLeftProgObjs());
    }

    @JsonIgnore
    public String getRightCategoryCode() {
        return getCategoryCode(getToRightProgObjs());
    }

    @JsonIgnore
    private String getCategoryCode(List<RefProgObjSchema> refProgObjs) {
        if (refProgObjs == null || refProgObjs.isEmpty()) {
            return null;
        }
        for (RefProgObjSchema refProgObj : refProgObjs) {
            if (refProgObj.getProgObjType().equals(RefProgObjSchema.PROG_OBJ_TYPE_PARTCATEGORY)) {
                return refProgObj.getProgObjCode();
            }
        }
        return null;
    }

}
