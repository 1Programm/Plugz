package com.programm.plugz.api.auto;

import com.programm.plugz.api.Config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be annotated on final fields or methods and can only be used in classes annotated by the {@link Config} annotation.
 * Marks a value as a configuration value.
 * This value is the last step in the config phase, so it would override existing values.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigValue {

    /**
     * A qualifier for the annotated field or method.
     * So it will be saved under the specified qualifier.
     */
    String value();

}
