package com.programm.projects.plugz.db.abstractbase.entity;

import java.util.Map;

public class EntityEntry {

    private final Class<?> entityCls;
    private final Class<?> idCls;
    private final Map<String, FieldEntry> fieldEntryMap;

    public EntityEntry(Class<?> entityCls, Class<?> idCls, Map<String, FieldEntry> fieldEntryMap) {
        this.entityCls = entityCls;
        this.idCls = idCls;
        this.fieldEntryMap = fieldEntryMap;
    }

    public Class<?> getEntityCls() {
        return entityCls;
    }

    public Class<?> getIdCls() {
        return idCls;
    }

    public Map<String, FieldEntry> getFieldEntryMap() {
        return fieldEntryMap;
    }
}
