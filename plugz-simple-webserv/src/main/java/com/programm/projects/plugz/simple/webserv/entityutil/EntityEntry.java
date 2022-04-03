package com.programm.projects.plugz.simple.webserv.entityutil;

import java.util.Map;

public class EntityEntry {

    private final Class<?> entityCls;
    private final Map<String, FieldEntry> fieldEntryMap;
    private final int classModifiers;

    public EntityEntry(Class<?> entityCls, Map<String, FieldEntry> fieldEntryMap, int classModifiers) {
        this.entityCls = entityCls;
        this.fieldEntryMap = fieldEntryMap;
        this.classModifiers = classModifiers;
    }

    public Class<?> getEntityCls() {
        return entityCls;
    }

    public Map<String, FieldEntry> getFieldEntryMap() {
        return fieldEntryMap;
    }

    public int getClassModifiers() {
        return classModifiers;
    }
}
