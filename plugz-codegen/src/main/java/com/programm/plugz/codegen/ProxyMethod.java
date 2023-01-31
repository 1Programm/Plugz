package com.programm.plugz.codegen;

import java.lang.reflect.InvocationTargetException;

public interface ProxyMethod {

    String getName();

    Object invokeOrig(Object instance, Object... args) throws InvocationTargetException;

    Object invokeProxy(Object instance, Object... args) throws InvocationTargetException;

}
