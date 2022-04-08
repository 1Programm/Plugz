package com.programm.plugz.api.auto;

import com.programm.plugz.annocheck.Check;
import com.programm.plugz.api.Config;
import com.programm.plugz.api.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Will get an instance for the annotated field or setter method in the discovering phase.
 * An annotated parameter is called a magic - parameter and if this method is invoked as a magic method so that the system will invoke the method it will automatically set the annotated parameters.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Check(clsAnnotatedWith = { Config.class, Service.class })
public @interface Get {

    /**
     * Important for the setup phase.
     * After the PreSetup phase all required fields or methods which are specified as required and could not find a provider for their annotated type will throw an exception.
     * Will result in null or the default value for primitives if set to false and no instance could be found.
     */
    boolean required() default true;

}
