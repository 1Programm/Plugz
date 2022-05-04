package com.programm.plugz.webserv.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to tell that the annotated parameter should be a request-parameter with the name [value].
 * If value is left empty it will try to get the nth request-parameter corresponding to the position of this annotated parameter in the method.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {

    /**
     * @return The name of the request-parameter.
     */
    String value() default "";

}
