package com.jmix.executor.imodel.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 动态属性继承注解
 * 用于标记动态属性的继承关系，支持属性重写
 *
 * @since 2025-12-27
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DAttrInherit {
    /**
     * 父对象编码
     */
    String fatherCode() default "";

    /**
     * 重写属性列表，格式如："Speed:instType=1","Capacity:instType=1"
     */
    String[] overrideAttrs() default {};
}
