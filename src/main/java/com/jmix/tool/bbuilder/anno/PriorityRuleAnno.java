package com.jmix.tool.bbuilder.anno;

import com.jmix.executor.bmodel.logic.PriorityStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 优先级规则注解，用于标记优先级规则方法
 * 
 * @since 2025-01-XX
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PriorityRuleAnno {
    /**
     * 规则代码
     */
    String code() default "";

    /**
     * 自然语言描述
     */
    String normalNaturalCode() default "";

    /**
     * 原始代码
     */
    String rawCode() default "";

    /**
     * 父部件编码，如果为null或空字符串，则规则添加到Module中；否则添加到对应的PartCategory中
     */
    String fatherCode() default "";

    /**
     * 属性代码
     */
    String attrCode() default "";

    /**
     * 优先级策略
     */
    PriorityStrategy strategy() default PriorityStrategy.MAX;
}
