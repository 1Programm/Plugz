package com.programm.plugz.api.lifecycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The third place in the Magic-Methods system - lifecycle.
 * Methods annotated with this annotation will be called after the discovering phase and all subsystems started up.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostStartup {
}
