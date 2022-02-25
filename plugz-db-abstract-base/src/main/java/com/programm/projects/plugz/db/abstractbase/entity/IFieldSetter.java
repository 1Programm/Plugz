package com.programm.projects.plugz.db.abstractbase.entity;

import java.lang.reflect.InvocationTargetException;

public interface IFieldSetter {

    void set(Object instance, Object data) throws InvocationTargetException;

}
