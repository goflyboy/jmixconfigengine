package com.jmix.tool.bbuilder.anno;

import com.jmix.executor.bmodel.logic.BusinessRelationType;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.StructCompareOperator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for declaring a ternary structured rule in product metadata.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TripleStructRuleAnno {
    String code() default "";

    String normalNaturalCode() default "";

    String fatherCode() default "";

    String parentRuleCode() default "";

    String expr1ObjectCode();

    String expr1AttrCode();

    StructCompareOperator expr1Operator() default StructCompareOperator.EQ;

    String[] expr1Values();

    BusinessRelationType relationType() default BusinessRelationType.CO_DEPENDENT;

    String expr2ObjectCode();

    String expr2AttrCode();

    StructCompareOperator expr2Operator() default StructCompareOperator.EQ;

    String[] expr2Values();

    String expr3ObjectCode();

    String expr3AttrCode();

    StructCompareOperator expr3Operator() default StructCompareOperator.EQ;

    String[] expr3Values();

    CalcStage calcStage() default CalcStage.MID;
}
