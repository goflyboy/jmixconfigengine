package com.jmix.tool.bbuilder.anno;

import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.PartCombinationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for declaring a structured combination parent rule.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CombinationStructRuleAnno {
    String code() default "";

    String normalNaturalCode() default "";

    String fatherCode() default "";

    int arity();

    String[] dimensionCategoryCodes();

    PartCombinationType combinationType() default PartCombinationType.WHITE;

    String[] subRuleCodes() default {};

    CalcStage calcStage() default CalcStage.MID;
}
