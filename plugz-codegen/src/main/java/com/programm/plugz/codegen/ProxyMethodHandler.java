package com.programm.plugz.codegen;

import java.lang.reflect.Method;

public interface ProxyMethodHandler {

    boolean canHandle(Object o, Method method);

    Object invoke(Object o, ProxyMethod method, Object... args) throws Exception;


}
