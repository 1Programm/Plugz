package com.programm.plugz.api;

import com.programm.plugz.annocheck.Check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to allow the discovering phase to find this class and instantiate it
 * while invoking its magic methods and setting up auto values.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Check(clsNotAnnotatedWith = Config.class)
public @interface Service {
}
