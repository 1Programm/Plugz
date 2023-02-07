package com.programm.plugz.cls.analyzer;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
public class AnalyzedPropertyClass {

    AnalyzedParameterizedType type;
    Map<String, PropertyEntry> fieldEntryMap;
    IClassPropertyBuilder builder;

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
