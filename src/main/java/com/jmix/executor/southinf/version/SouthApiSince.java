package com.jmix.executor.southinf.version;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a southbound API type or method as available since the specified version.
 * All southbound API interfaces and methods must be annotated with this.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SouthApiSince {

    String value();
}
