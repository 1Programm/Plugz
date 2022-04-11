package com.programm.plugz.api;

public interface PlugzConfig {

    String profile();

    void registerConfiguration(String key, Object value);


    <T> T get(String name, Class<T> cls);

    default <T> T getOrDefault(String name, Class<T> cls, T defaultValue) {
        T value = get(name, cls);
        if(value == null) return defaultValue;
        return value;
    }

    default boolean getBoolean(String name){
        return get(name, Boolean.class);
    }

    default boolean getBooleanOrDefault(String name, boolean defaultValue){
        return getOrDefault(name, Boolean.class, defaultValue);
    }

    default byte getByte(String name){
        return get(name, Byte.class);
    }

    default byte getByteOrDefault(String name, byte defaultValue){
        return getOrDefault(name, Byte.class, defaultValue);
    }

    default short getShort(String name){
        return get(name, Short.class);
    }

    default short getShortOrDefault(String name, short defaultValue){
        return getOrDefault(name, Short.class, defaultValue);
    }

    default int getInt(String name){
        return get(name, Integer.class);
    }

    default int getIntOrDefault(String name, int defaultValue){
        return getOrDefault(name, Integer.class, defaultValue);
    }

    default long getLong(String name){
        return get(name, Long.class);
    }

    default long getLongOrDefault(String name, long defaultValue){
        return getOrDefault(name, Long.class, defaultValue);
    }

    default float getFloat(String name){
        return get(name, Float.class);
    }

    default float getFloatOrDefault(String name, float defaultValue){
        return getOrDefault(name, Float.class, defaultValue);
    }

    default double getDouble(String name){
        return get(name, Double.class);
    }

    default double getDoubleOrDefault(String name, double defaultValue){
        return getOrDefault(name, Double.class, defaultValue);
    }

    default char getChar(String name){
        return get(name, Character.class);
    }

    default char getCharOrDefault(String name, char defaultValue){
        return getOrDefault(name, Character.class, defaultValue);
    }

    default String get(String name){
        return get(name, String.class);
    }

    default String getOrDefault(String name, String defaultValue){
        return getOrDefault(name, String.class, defaultValue);
    }

}
