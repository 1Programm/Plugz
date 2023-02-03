package com.programm.plugz.codegen;

import java.lang.reflect.InvocationTargetException;

public interface ProxyMethod {

    String getName();

    Object invokeSuper(Object instance, Object... args) throws InvocationTargetException;

}
