package com.jmix.configengine.scenario.base;

import com.jmix.configengine.model.Para;
import com.jmix.configengine.model.ParaType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 参数注解，用于标记参数变量
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParaAnno {
    /**
     * 参数编码
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
     * 参数类型
     */
    ParaType type() default ParaType.ENUM;
    
    /**
     * 枚举选项代码列表
     */
    String[] options() default {};
    
    /**
     * 最小值（Range类型）
     */
    String minValue() default Para.DEFAULT_MIN_VALUE;//TODO:调成整数型？
    
    /**
     * 最大值（Range类型）
     */
    String maxValue() default Para.DEFAULT_MAX_VALUE;
    
    /**
     * 扩展模式
     */
    String extSchema() default "";
    
    /**
     * 扩展属性
     */
    String[] extAttrs() default {};
} 