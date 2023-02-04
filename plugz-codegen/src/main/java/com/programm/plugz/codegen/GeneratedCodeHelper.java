package com.programm.plugz.codegen;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * This class is used by generated proxy class code to wrap a proxy method so the super method can easily be called.
 */
public class GeneratedCodeHelper {

    @RequiredArgsConstructor
    private static class ProxyMethodImpl implements ProxyMethod {
        private final String methodName;
        private final Function<Object[], Object> superCall;

        @Override
        public String getName() {
            return methodName;
        }

        @Override
        public Object invokeSuper(Object instance, Object... args) {
            return superCall.apply(args);
        }
    }

    private static final Map<Class<?>, Map<Method, ProxyMethod>> PROXY_METHOD_HANDLE_MAP = new HashMap<>();

    public static ProxyMethod wrapMethod(Class<?> superClass, Method method, Function<Object[], Object> superCall) {
        Map<Method, ProxyMethod> proxyMethodMap = PROXY_METHOD_HANDLE_MAP.computeIfAbsent(superClass, k -> new HashMap<>());
        ProxyMethod methodHandle = proxyMethodMap.get(method);

        if(methodHandle != null) return methodHandle;

        methodHandle =  new ProxyMethodImpl(method.getName(), superCall);
        proxyMethodMap.put(method, methodHandle);
        return methodHandle;
    }

}
