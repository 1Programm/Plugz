package com.programm.plugz.api;

public interface PlugzConfig {

    String profile();

    <T> T get(String name);

    default <T> T getOrDefault(String name, T defaultValue) {
        T value = get(name);
        if(value == null) return defaultValue;
        return value;
    }

}
