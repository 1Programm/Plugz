package com.programm.plugz.api.condition;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Conditionals.class)
public @interface Conditional {

    String value();

}
