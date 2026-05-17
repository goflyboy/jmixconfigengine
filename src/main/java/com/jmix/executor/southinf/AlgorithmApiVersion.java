package com.jmix.executor.southinf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the southbound API version used by an algorithm source class.
 * 
 * @deprecated Use {@link com.jmix.tool.bbuilder.anno.AlgorithmApiVersion} instead.
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AlgorithmApiVersion {

    String southApiVersion();

    String algorithmVersion() default "";
}
