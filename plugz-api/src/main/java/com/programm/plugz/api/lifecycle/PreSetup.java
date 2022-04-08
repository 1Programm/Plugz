package com.programm.plugz.api.lifecycle;

import com.programm.plugz.annocheck.Check;
import com.programm.plugz.api.Config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The first Magic-Methods which are called.
 * Methods of Services or otherwise discovered classes with this annotation will be invoked as soon as they are instantiated.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Check(clsNotAnnotatedWith = Config.class)
public @interface PreSetup {
}
