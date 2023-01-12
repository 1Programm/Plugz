package com.programm.plugz.cls.analyzer;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class AnalyzedPropertyClass {

    private final AnalyzedParameterizedType type;
    private final Map<String, PropertyEntry> fieldEntryMap;
    private final IClassPropertyBuilder builder;

    public Class<?> getType() {
        return type.getType();
    }

    public AnalyzedParameterizedType getParameterizedType() {
        return type;
    }

    public Map<String, AnalyzedParameterizedType> getParameterizedTypeMap() {
        return type.getParameterizedTypeMap();
    }

    public AnalyzedParameterizedType getParameterizedType(String name){
        return type.getParameterizedType(name);
    }

    public Map<String, PropertyEntry> getFieldEntryMap() {
        return fieldEntryMap;
    }

    public IClassPropertyBuilder getBuilder() {
        return builder;
    }
}
