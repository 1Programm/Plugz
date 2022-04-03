package com.programm.plugz.api.auto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Register the annotated method as a provider for the returning type.
 * Counter-part for the @{@link Get} annotation.
 * The annotated method is a magic method and can have magic parameters but no non - magic parameters.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Set {

    /**
     * If set to false the method will be called for every time it is needed.
     * If set to true the method will be called once and will register its value as a constant magic instance.
     */
    boolean persist() default false;

}
