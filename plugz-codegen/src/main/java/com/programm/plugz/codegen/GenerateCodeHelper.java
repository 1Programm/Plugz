package com.programm.plugz.codegen;

import lombok.RequiredArgsConstructor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GenerateCodeHelper {

    @RequiredArgsConstructor
    private static class ProxyMethodImpl implements ProxyMethod {
        private final Method method;
        private final MethodHandle methodHandle;

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public Object invokeOrig(Object instance, Object... args) throws InvocationTargetException {
            try {
                return methodHandle.bindTo(instance).invokeWithArguments(args);
            }
            catch (InvocationTargetException e){
                throw e;
            }
            catch (Throwable e){
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public Object invokeProxy(Object instance, Object... args) throws InvocationTargetException {
            try {
                return method.invoke(args);
            }
            catch (IllegalAccessException e){
                throw new IllegalStateException("INVALID STATE: Should be accessible!", e);
            }
        }
    }

    public static ProxyMethod wrapMethod(Class<?> superClass, Class<?> caller, Method method) {
        try {
            MethodHandle handle = MethodHandles.privateLookupIn(superClass, MethodHandles.lookup()).findSpecial(superClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()), superClass);
            return new ProxyMethodImpl(method, handle);
        }
        catch (NoSuchMethodException e){
            throw new IllegalStateException("INVALID STATE", e);
        }
        catch (IllegalAccessException e){
            throw new IllegalStateException("INVALID STATE...", e);
        }

    }

}
