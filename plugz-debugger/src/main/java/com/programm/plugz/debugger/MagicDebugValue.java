package com.programm.plugz.debugger;

import java.lang.reflect.Field;

class MagicDebugValue {

    public final Object instance;
    public final Field field;
    public final boolean needsAccess;
    public final String name;
    public final DValue<?> dValueInstance;
    public final MagicDebugValue[] children;

    public MagicDebugValue(Object instance, Field field, boolean needsAccess, String name, DValue<?> dValueInstance, MagicDebugValue[] children) {
        this.instance = instance;
        this.field = field;
        this.needsAccess = needsAccess;
        this.name = name;
        this.dValueInstance = dValueInstance;
        this.children = children;
    }
}
