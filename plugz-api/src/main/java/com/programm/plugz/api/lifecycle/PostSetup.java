package com.programm.plugz.api.lifecycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The second set of Magic-Methods in the lifecycle.
 * Will be called after all wait - dependencies have been resolved and checked.
 * So after the discovering phase.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostSetup {
}
