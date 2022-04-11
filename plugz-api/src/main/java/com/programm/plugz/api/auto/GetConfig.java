package com.programm.plugz.api.auto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Counter-part to the {@link SetConfig} annotation.
 * Will automatically get the configuration value for the specified qualifier.
 * Acts the same as the {@link Get} annotation but only for configuration values.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GetConfig {

    String value();

}
