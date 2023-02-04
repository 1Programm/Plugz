package com.programm.plugz.codegen;

import java.lang.reflect.Method;

/**
 * A method handler for proxies which will be called before any super method is invoked.
 */
public interface ProxyMethodHandler {

    /**
     * Method to check if some method should be handled by the {@link #invoke(Object, ProxyMethod, Object...)} method.
     * @param instance the instance.
     * @param method the method that is being called.
     * @return true if the method should be handeled.
     */
    boolean canHandle(Object instance, Method method);

    /**
     * This method is called if the {@link #canHandle(Object, Method)} method returned true.
     * It is called instead of simply calling the super method for this proxy.
     * @param instance the instance.
     * @param method the method that is being called.
     * @param args the method arguments.
     * @return the method return value.
     * @throws Exception if some exception occured.
     */
    Object invoke(Object instance, ProxyMethod method, Object... args) throws Exception;

}
