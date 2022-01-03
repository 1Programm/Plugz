package com.programm.projects.plugz.magic.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Value {

    /**
     * @return the name of the resource - field.
     * If left empty the field name will be used.
     */
    String value() default "";

    /**
     * @return the fallback strategy for this value.
     * If the value is IResourceFallback it will throw an error if the fallback is needed.
     */
    Class<? extends IValueFallback> fallback() default IValueFallback.class;

}
