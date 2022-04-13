package com.programm.plugz.cls.analyzer;

import java.util.Map;

public class AnalyzedPropertyClass {

    private final Class<?> entityCls;
    private final Map<String, PropertyEntry> fieldEntryMap;
    private final int classModifiers;

    public AnalyzedPropertyClass(Class<?> entityCls, Map<String, PropertyEntry> fieldEntryMap, int classModifiers) {
        this.entityCls = entityCls;
        this.fieldEntryMap = fieldEntryMap;
        this.classModifiers = classModifiers;
    }

    public Class<?> getEntityCls() {
        return entityCls;
    }

    public Map<String, PropertyEntry> getFieldEntryMap() {
        return fieldEntryMap;
    }

    public int getClassModifiers() {
        return classModifiers;
    }
}
