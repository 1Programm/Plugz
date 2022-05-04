package com.programm.plugz.webserv.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to register a method as put-mapping-method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PutMapping {

    /**
     * The mapping path which will be appended to the parent class root path specified in the {@link RestController} annotation.
     * @return The path.
     */
    String value() default "";

    /**
     * A specific content type which will be used to parse the return value of the annotated method.
     * If left empty the default content type for the parent class, specified in the {@link RestController} annotation, will be used.
     * @return The content type for the response.
     */
    String contentType() default "";

}
