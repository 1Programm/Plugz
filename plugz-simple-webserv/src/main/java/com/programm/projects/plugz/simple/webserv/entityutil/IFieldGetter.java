package com.programm.projects.plugz.simple.webserv.entityutil;

import java.lang.reflect.InvocationTargetException;

public interface IFieldGetter {

    Object get(Object instance) throws InvocationTargetException;

    int modifiers();
    
}
