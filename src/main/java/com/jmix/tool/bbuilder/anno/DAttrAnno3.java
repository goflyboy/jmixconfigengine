package com.jmix.tool.bbuilder.anno;

import com.jmix.executor.bmodel.attr.DynamicAttributeType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 动态属性注解3
 * 用于标记动态属性定义
 *
 * @since 2025-12-27
 */
@Target({ ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface DAttrAnno3 {
    /**
     * 属性编码
     */
    String code() default "";

    /**
     * 扩展模式
     */
    String optionExtSchema() default "";

    /**
     * 可选值列表
     */
    String[] options() default {};

    /**
     * 实例类型
     */
    int instType() default 0;

    /**
     * 动态属性类型
     */
    DynamicAttributeType dynAttrType() default DynamicAttributeType.E_INT;
}
