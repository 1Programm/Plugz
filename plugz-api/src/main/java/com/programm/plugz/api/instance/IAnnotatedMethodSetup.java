package com.programm.plugz.api.instance;

import com.programm.plugz.api.MagicInstanceException;
import com.programm.plugz.api.PlugzConfig;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface IAnnotatedMethodSetup <T extends Annotation> {

    @SuppressWarnings("unchecked")
    default void _setup(Object annotation, Object instance, Method method, IInstanceManager manager) throws MagicInstanceException{
        setup((T)annotation, instance, method, manager);
    }

    void setup(T annotation, Object instance, Method method, IInstanceManager manager) throws MagicInstanceException;

}
