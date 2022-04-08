package com.programm.plugz.api;

import com.programm.plugz.annocheck.Check;
import com.programm.plugz.api.auto.ConfigValue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Check(notAnnotatedWith = ConfigValue.class)
public @interface Async {

    long delay() default 0L;

}