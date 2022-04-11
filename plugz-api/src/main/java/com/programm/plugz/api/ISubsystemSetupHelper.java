package com.programm.plugz.api;

import com.programm.plugz.api.instance.IAnnotatedClassSetup;
import com.programm.plugz.api.instance.IAnnotatedFieldSetup;
import com.programm.plugz.api.instance.IAnnotatedMethodSetup;

import java.lang.annotation.Annotation;

public interface ISubsystemSetupHelper {

    <T extends Annotation> void registerClassAnnotation(Class<T> cls, IAnnotatedClassSetup<T> setup);

    <T extends Annotation> void registerFieldAnnotation(Class<T> cls, IAnnotatedFieldSetup<T> setup);

    <T extends Annotation> void registerMethodAnnotation(Class<T> cls, IAnnotatedMethodSetup<T> setup);

    void registerInstance(Class<?> cls, Object instance) throws MagicInstanceException;
}
