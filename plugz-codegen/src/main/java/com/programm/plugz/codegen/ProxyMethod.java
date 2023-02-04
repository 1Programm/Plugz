package com.programm.plugz.codegen;

import java.lang.reflect.InvocationTargetException;

/**
 * Method wrapper for proxies to call their super methods.
 */
public interface ProxyMethod {

    /**
     * @return the name of the method this is a wrapper for.
     */
    String getName();

    /**
     * Invokes the super method.
     * @param instance the instance this method should be invoked on.
     * @param args method arguments.
     * @return the super method return value.
     * @throws InvocationTargetException if the invocation of the super method throws an exception.
     */
    Object invokeSuper(Object instance, Object... args) throws InvocationTargetException;

}
