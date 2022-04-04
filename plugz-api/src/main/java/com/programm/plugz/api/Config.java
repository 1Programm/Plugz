package com.programm.plugz.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The class annotated with this annotation will be discovered configuration phase
 * and can the last step in the configuration phase so all values that are declared in an plugz config file
 * or as program args will be overridden.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {

    /**
     * The name of the configuration.
     * (main, develop, test, usw. ...)
     * If left empty it will use the main configuration.
     * If the name doesn't match the active configuration name it will not be instantiated and used to set configuration values.
     */
    String value() default "";

}
