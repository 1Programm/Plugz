package com.programm.plugz.cls.analyzer;

import java.lang.reflect.InvocationTargetException;

public interface IClassPropertyGetter {

    Object get(Object instance) throws InvocationTargetException;

    int modifiers();

}
