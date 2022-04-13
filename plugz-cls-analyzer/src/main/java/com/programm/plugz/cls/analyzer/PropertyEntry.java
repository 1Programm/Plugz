package com.programm.plugz.cls.analyzer;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;

@RequiredArgsConstructor
public class PropertyEntry {

    private final Class<?> type;
    private final Field field;
    IClassPropertyGetter getter;
    IClassPropertySetter setter;

    public Class<?> getType() {
        return type;
    }

    public Field getField() {
        return field;
    }

    public IClassPropertyGetter getGetter() {
        return getter;
    }

    public IClassPropertySetter getSetter() {
        return setter;
    }
}
