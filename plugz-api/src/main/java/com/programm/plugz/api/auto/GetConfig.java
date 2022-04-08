package com.programm.plugz.api.auto;

import com.programm.plugz.annocheck.Check;
import com.programm.plugz.api.Config;
import com.programm.plugz.api.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Counter-part to the {@link ConfigValue} annotation.
 * Will automatically get the configuration value for the specified qualifier.
 * Acts the same as the {@link Get} annotation but only for configuration values.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Check(clsAnnotatedWith = { Config.class, Service.class })
public @interface GetConfig {

    String value();

}
