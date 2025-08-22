package com.jmix.configengine.scenario.base;

import com.jmix.configengine.model.PartType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 部件注解，用于标记部件变量
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PartAnno {
    /**
     * 部件编码
     */
    String code() default "";
    
    /**
     * 父对象编码
     */
    String fatherCode() default "";
    
    /**
     * 默认值
     */
    String defaultValue() default "";
    
    /**
     * 描述信息
     */
    String description() default "";
    
    /**
     * 排序号
     */
    int sortNo() default 0;
    
    /**
     * 部件类型
     */
    PartType type() default PartType.ATOMIC;
    
    /**
     * 价格
     */
    long price() default 0L;
    
    /**
     * 规格属性
     */
    String[] attrs() default {};
    
    /**
     * 扩展模式
     */
    String extSchema() default "";
    
    /**
     * 扩展属性
     */
    String[] extAttrs() default {};
} 