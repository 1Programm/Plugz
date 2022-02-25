package com.programm.projects.plugz.db.abstractbase.entity;

public class FieldEntry {

    private final Class<?> type;
    IFieldGetter getter;
    IFieldSetter setter;

    public FieldEntry(Class<?> type) {
        this.type = type;
    }

    public Class<?> getType() {
        return type;
    }

    public IFieldGetter getGetter() {
        return getter;
    }

    public IFieldSetter getSetter() {
        return setter;
    }
}
