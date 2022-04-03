package com.programm.projects.plugz.magic.api;

public interface SysArgs {

    Object get(String name);

    <T> T get(String name, Class<T> cls);

    <T> T getDefault(String name, T defaultValue);

    String[] getOriginal();

}
