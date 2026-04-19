package com.jmix.tool.bbuilder.anno;

import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.Cardinality;
import com.jmix.executor.bmodel.logic.EffectScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 代码规则注解，用于标记代码规则方法
 * 
 * @since 2025-09-22
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CodeRuleAnno {
    /**
     * 规则代码
     */
    String code() default "";

    /**
     * 自然语言描述
     */
    String normalNaturalCode() default "";

    /**
     * 父部件编码，如果为null或空字符串，则规则添加到Module中；否则添加到对应的PartCategory中
     */
    String fatherCode() default "";

    /**
     * 计算阶段
     * 用于指定规则在哪个计算阶段执行
     */
    CalcStage calcStage() default CalcStage.MID;

    /**
     * 左侧编程对象描述字符串
     * 格式：progObjCode:progObjField|progObjField
     * 例如："drive:Select|Quantity"
     */
    String leftProObjsStr() default "";

    /**
     * 右侧编程对象描述字符串
     * 格式：progObjCode:progObjField|progObjField
     * 例如："drive:Select|Quantity"
     */
    String rightProObjsStr() default "";

    /**
     * 作用范围
     */
    EffectScope effectScope() default EffectScope.SingleInst;

    /**
     * 左侧基数
     */
    Cardinality leftCardinality() default Cardinality.ONE;

    /**
     * 右侧基数
     */
    Cardinality rightCardinality() default Cardinality.ONE;

    /**
     * 属性参数编码
     * 格式：attrCode:Type,attrCode:Type
     * 例如："Capacity:SumSum,Quantity:SumSum"
     */
    String attrParaCodes() default "";
}