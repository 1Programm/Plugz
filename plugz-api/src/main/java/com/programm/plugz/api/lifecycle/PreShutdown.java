package com.programm.plugz.api.lifecycle;

import com.programm.plugz.annocheck.Check;
import com.programm.plugz.api.Config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The second last set of Magic-Methods to be called in the system lifecycle.
 * Will be called before the subsystems are shut down so all functionality will be still there.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Check(clsNotAnnotatedWith = Config.class)
public @interface PreShutdown {
}
