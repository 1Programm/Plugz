package com.programm.plugz.api;

import com.programm.plugz.api.instance.IAnnotatedClassSetup;
import com.programm.plugz.api.instance.IAnnotatedFieldSetup;
import com.programm.plugz.api.instance.IAnnotatedMethodSetup;
import com.programm.plugz.api.instance.ISearchClassSetup;

import java.lang.annotation.Annotation;

public interface ISubsystemSetupHelper {

    void registerSearchClass(Class<?> cls, ISearchClassSetup setup);

    <T extends Annotation> void registerClassAnnotation(Class<T> cls, IAnnotatedClassSetup<T> setup);

    <T extends Annotation> void registerFieldAnnotation(Class<T> cls, IAnnotatedFieldSetup<T> setup);

    <T extends Annotation> void registerMethodAnnotation(Class<T> cls, IAnnotatedMethodSetup<T> setup);

    void registerInstance(Class<?> cls, Object instance) throws MagicInstanceException;
}
