package com.programm.projects.plugz.simple.webserv.entityutil;

import java.lang.reflect.InvocationTargetException;

public interface IFieldSetter {

    void set(Object instance, Object data) throws InvocationTargetException;

    int modifiers();

}
