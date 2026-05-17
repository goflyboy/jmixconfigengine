package com.jmix.tool.bbuilder.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the southbound API version used by an algorithm source class.
 * This annotation is for source code generation and artifact building purposes,
 * not a runtime southbound API.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AlgorithmApiVersion {

    String southApiVersion();

    String algorithmVersion() default "";
}
