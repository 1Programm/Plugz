package com.programm.plugz.api.instance;

import com.programm.plugz.api.MagicInstanceException;
import com.programm.plugz.api.PlugzConfig;

import java.lang.annotation.Annotation;

public interface IAnnotatedClassSetup <T extends Annotation> {

    @SuppressWarnings("unchecked")
    default void _setup(Object annotation, Class<?> cls, IInstanceManager manager) throws MagicInstanceException {
        setup((T)annotation, cls, manager);
    }

    void setup(T annotation, Class<?> cls, IInstanceManager manager) throws MagicInstanceException;

}
