package com.programm.plugz.api;

import java.util.function.Function;
import java.util.function.Supplier;

public interface PlugzConfig {

    String profile();

    void registerDefaultConfiguration(String key, Object value);

    void registerConfiguration(String key, Object value);




    <T> T get(String name, Class<T> cls);

    default <T, E extends Throwable> T getOrError(String name, Class<T> cls, Supplier<E> ex) throws E {
        T val = get(name, cls);
        if(val == null) throw ex.get();
        return val;
    }

    default <T, E extends Throwable> T getOrError(String name, Class<T> cls, Function<String, E> ex) throws E {
        T val = get(name, cls);
        if(val == null) throw ex.apply("Failed to get configuration [" + name + "]!");
        return val;
    }

    default <T> T getOrRegisterDefault(String key, Class<T> cls, T value){
        registerDefaultConfiguration(key, value);
        return get(key, cls);
    }

    default <T> T getOrDefault(String name, Class<T> cls, T defaultValue) {
        T value = get(name, cls);
        if(value == null) return defaultValue;
        return value;
    }




    default Boolean getBool(String name){
        return get(name, Boolean.class);
    }

    default <E extends Throwable> Boolean getBoolOrError(String name, Supplier<E> ex) throws E {
        return getOrError(name, Boolean.class, ex);
    }

    default <E extends Throwable> Boolean getBoolOrError(String name, Function<String, E> ex) throws E {
        return getOrError(name, Boolean.class, ex);
    }

    default Boolean getBoolOrRegisterDefault(String key, Boolean defaultValue){
        return getOrRegisterDefault(key, Boolean.class, defaultValue);
    }

    default Boolean getBoolOrDefault(String name, boolean defaultValue){
        return getOrDefault(name, Boolean.class, defaultValue);
    }




    default Byte getByte(String name){
        return get(name, Byte.class);
    }

    default <E extends Throwable> Byte getByteOrError(String name, Supplier<E> ex) throws E {
        return getOrError(name, Byte.class, ex);
    }

    default <E extends Throwable> Byte getByteOrError(String name, Function<String, E> ex) throws E {
        return getOrError(name, Byte.class, ex);
    }

    default Byte getByteOrRegisterDefault(String key, byte defaultValue){
        return getOrRegisterDefault(key, Byte.class, defaultValue);
    }

    default Byte getByteOrDefault(String name, byte defaultValue){
        return getOrDefault(name, Byte.class, defaultValue);
    }




    default Short getShort(String name){
        return get(name, Short.class);
    }

    default <E extends Throwable> Short getShortOrError(String name, Supplier<E> ex) throws E {
        return getOrError(name, Short.class, ex);
    }

    default <E extends Throwable> Short getShortOrError(String name, Function<String, E> ex) throws E {
        return getOrError(name, Short.class, ex);
    }

    default Short getShortOrRegisterDefault(String key, short defaultValue){
        return getOrRegisterDefault(key, Short.class, defaultValue);
    }

    default Short getShortOrDefault(String name, short defaultValue){
        return getOrDefault(name, Short.class, defaultValue);
    }




    default Integer getInt(String name){
        return get(name, Integer.class);
    }

    default <E extends Throwable> Integer getIntOrError(String name, Supplier<E> ex) throws E {
        return getOrError(name, Integer.class, ex);
    }

    default <E extends Throwable> Integer getIntOrError(String name, Function<String, E> ex) throws E {
        return getOrError(name, Integer.class, ex);
    }

    default Integer getIntOrRegisterDefault(String key, int defaultValue){
        return getOrRegisterDefault(key, Integer.class, defaultValue);
    }

    default Integer getIntOrDefault(String name, int defaultValue){
        return getOrDefault(name, Integer.class, defaultValue);
    }




    default Long getLong(String name){
        return get(name, Long.class);
    }

    default <E extends Throwable> Long getLongOrError(String name, Supplier<E> ex) throws E {
        return getOrError(name, Long.class, ex);
    }

    default <E extends Throwable> Long getLongOrError(String name, Function<String, E> ex) throws E {
        return getOrError(name, Long.class, ex);
    }

    default Long getLongOrRegisterDefault(String key, long defaultValue){
        return getOrRegisterDefault(key, Long.class, defaultValue);
    }

    default Long getLongOrDefault(String name, long defaultValue){
        return getOrDefault(name, Long.class, defaultValue);
    }




    default Float getFloat(String name){
        return get(name, Float.class);
    }

    default <E extends Throwable> Float getFloatOrError(String name, Supplier<E> ex) throws E {
        return getOrError(name, Float.class, ex);
    }

    default <E extends Throwable> Float getFloatOrError(String name, Function<String, E> ex) throws E {
        return getOrError(name, Float.class, ex);
    }

    default Float getFloatOrRegisterDefault(String key, float defaultValue){
        return getOrRegisterDefault(key, Float.class, defaultValue);
    }

    default Float getFloatOrDefault(String name, float defaultValue){
        return getOrDefault(name, Float.class, defaultValue);
    }




    default Double getDouble(String name){
        return get(name, Double.class);
    }

    default <E extends Throwable> Double getDoubleOrError(String name, Supplier<E> ex) throws E {
        return getOrError(name, Double.class, ex);
    }

    default <E extends Throwable> Double getDoubleOrError(String name, Function<String, E> ex) throws E {
        return getOrError(name, Double.class, ex);
    }

    default Double getDoubleOrRegisterDefault(String key, double defaultValue){
        return getOrRegisterDefault(key, Double.class, defaultValue);
    }

    default Double getDoubleOrDefault(String name, double defaultValue){
        return getOrDefault(name, Double.class, defaultValue);
    }




    default Character getChar(String name){
        return get(name, Character.class);
    }

    default <E extends Throwable> Character getCharOrError(String name, Supplier<E> ex) throws E {
        return getOrError(name, Character.class, ex);
    }

    default <E extends Throwable> Character getCharOrError(String name, Function<String, E> ex) throws E {
        return getOrError(name, Character.class, ex);
    }

    default Character getCharOrRegisterDefault(String key, char defaultValue){
        return getOrRegisterDefault(key, Character.class, defaultValue);
    }

    default Character getCharOrDefault(String name, char defaultValue){
        return getOrDefault(name, Character.class, defaultValue);
    }




    default String get(String name){
        return get(name, String.class);
    }

    default <E extends Throwable> String getOrError(String name, Supplier<E> ex) throws E {
        return getOrError(name, String.class, ex);
    }

    default <E extends Throwable> String getOrError(String name, Function<String, E> ex) throws E {
        return getOrError(name, String.class, ex);
    }

    default String getOrRegisterDefault(String key, String defaultValue){
        return getOrRegisterDefault(key, String.class, defaultValue);
    }

    default String getOrDefault(String name, String defaultValue){
        return getOrDefault(name, String.class, defaultValue);
    }

}
