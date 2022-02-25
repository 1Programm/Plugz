package com.programm.projects.plugz.db.abstractbase.entity;

import java.lang.reflect.InvocationTargetException;

public interface IFieldGetter {

    Object get(Object instance) throws InvocationTargetException;
    
}
