package com.jmix.configengine.scenario.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 模块注解，用于标记约束算法类
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleAnno {
    /**
     * 模块ID
     */
    long id();
    
    /**
     * 模块编码
     */
    String code() default "";
    
    /**
     * 包名
     */
    String packageName() default "";
    
    /**
     * 版本号
     */
    String version() default "1.0.0";
    
    /**
     * 描述信息
     */
    String description() default "";
    
    /**
     * 排序号
     */
    int sortNo() default 0;
    
    /**
     * 扩展模式
     */
    String extSchema() default "";
    
    /**
     * 扩展属性
     */
    String[] extAttrs() default {};
} 