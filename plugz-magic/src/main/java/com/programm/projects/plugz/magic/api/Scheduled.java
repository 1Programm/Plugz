package com.programm.projects.plugz.magic.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Scheduled {

    long repeat() default 0L;

    long startAfter() default 0L;

    long stopAfter() default 0L;

}
