package com.programm.plugz.api.instance;

import com.programm.plugz.api.MagicInstanceException;
import com.programm.plugz.api.PlugzConfig;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public interface IAnnotatedFieldSetup <T extends Annotation> {

    @SuppressWarnings("unchecked")
    default void _setup(Object annotation, Object instance, Field field, IInstanceManager manager) throws MagicInstanceException{
        setup((T)annotation, instance, field, manager);
    }

    void setup(T annotation, Object instance, Field field, IInstanceManager manager) throws MagicInstanceException;

}
