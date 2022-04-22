package com.programm.plugz.debugger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark a field as a DebugValue.
 * Will observe its value and display its values in runtime.
 * If the type of the field is not of DValue it may slow down the application if many are used.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DebugValue {

    /**
     * The visible name for the debug value.
     * If left empty the name of the field will be used.
     */
    String value() default "";

}
