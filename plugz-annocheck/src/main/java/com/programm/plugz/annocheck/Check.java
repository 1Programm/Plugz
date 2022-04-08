package com.programm.plugz.annocheck;

import java.lang.annotation.*;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Checks.class)
public @interface Check {

    ElementType[] target() default {};

    Class<? extends Annotation>[] clsAnnotatedWith() default {};
    Class<? extends Annotation>[] clsNotAnnotatedWith() default {};
    Class<?>[] clsImplementing() default {};
    Class<?>[] clsNotImplementing() default {};

    Class<? extends Annotation>[] annotatedWith() default {};
    Class<? extends Annotation>[] notAnnotatedWith() default {};

}
