package com.programm.plugz.cls.analyzer;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.util.Map;

@RequiredArgsConstructor
public class PropertyEntry {

    private final AnalyzedPropertyClass type;
    private final Field field;
    IClassPropertyGetter getter;
    IClassPropertySetter setter;

    public Class<?> getType() {
        return type.getType();
    }

    public AnalyzedPropertyClass getPropertyType() {
        return type;
    }

    public Map<String, AnalyzedParameterizedType> getParameterizedTypeMap() {
        return type.getParameterizedTypeMap();
    }

    public AnalyzedParameterizedType getParameterizedType(String name){
        return type.getParameterizedType(name);
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
