package com.programm.plugz.cls.analyzer;

import java.lang.reflect.InvocationTargetException;

public interface IClassPropertySetter {

    void set(Object instance, Object data) throws InvocationTargetException;

    int modifiers();

}
