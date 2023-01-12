package com.programm.plugz.api.instance;

import com.programm.plugz.api.MagicInstanceException;
import com.programm.plugz.api.auto.AutoWaitType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public interface IInstanceManager {

    boolean canWait();

    void instantiate(Class<?> cls) throws MagicInstanceException;

    void instantiate(Class<?> cls, MagicConsumer<Object> callback) throws MagicInstanceException;

    <T> T getInstance(Class<T> cls) throws MagicInstanceException;

    void registerInstance(Class<?> cls, Object instance) throws MagicInstanceException;

    Object getField(Field field, Object instance);

    void setField(Field field, Object instance, Object value);


    MagicMethod buildMagicMethod(Object instance, Method method);

    void waitForField(Class<?> type, Object instance, Field field, AutoWaitType waitType);

}
