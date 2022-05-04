package com.programm.plugz.webserv.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to declare a class as a rest controller.
 * Will be discovered and instantiated by the environment.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestController {

    /**
     * @return The base path mapping for the annotated class.
     */
    String value() default "";

    /**
     * The default content type for all return values for mapped methods in this annotated class.
     * The default will be used when the mapped method does not specify any specific content type.
     * @return The default content type for the annotated class.
     */
    String defaultContentType() default "application/json";

}
