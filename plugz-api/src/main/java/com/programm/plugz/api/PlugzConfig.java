package com.programm.plugz.api;

public interface PlugzConfig {

    String profile();

    <T> T get(String name);

    void registerConfiguration(String key, Object value);

    default <T> T getOrDefault(String name, T defaultValue) {
        T value = get(name);
        if(value == null) return defaultValue;
        return value;
    }

}
