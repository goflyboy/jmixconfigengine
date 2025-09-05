package com.jmix.configengine.scenario.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 代码规则注解，用于标记代码规则方法
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
} 