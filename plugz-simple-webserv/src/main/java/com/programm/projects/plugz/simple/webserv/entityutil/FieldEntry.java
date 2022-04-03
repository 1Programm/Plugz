package com.programm.projects.plugz.simple.webserv.entityutil;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;

@RequiredArgsConstructor
public class FieldEntry {

    private final Class<?> type;
    private final Field field;
    IFieldGetter getter;
    IFieldSetter setter;

    public Class<?> getType() {
        return type;
    }

    public Field getField() {
        return field;
    }

    public IFieldGetter getGetter() {
        return getter;
    }

    public IFieldSetter getSetter() {
        return setter;
    }
}
