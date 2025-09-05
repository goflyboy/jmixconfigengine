package com.jmix.configengine.scenario.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 兼容性规则注解，用于标记兼容性规则方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CompatiableRuleAnno {
    /**
     * 左表达式代码
     */
    String leftExprCode() default "";
    
    /**
     * 操作符
     */
    String operator() default "";
    
    /**
     * 右表达式代码
     */
    String rightExprCode() default "";
    
    /**
     * 自然语言描述
     */
    String normalNaturalCode() default "";
} 