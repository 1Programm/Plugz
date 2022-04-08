package com.programm.plugz.api.lifecycle;

import com.programm.plugz.annocheck.Check;
import com.programm.plugz.api.Config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The last set of Magic-Methods to be run in the systems lifecycle.
 * All subsystems have been shut down.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Check(clsNotAnnotatedWith = Config.class)
public @interface PostShutdown {
}
