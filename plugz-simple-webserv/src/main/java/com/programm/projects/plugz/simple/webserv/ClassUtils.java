package com.programm.projects.plugz.simple.webserv;

public class ClassUtils {

    public static boolean isPrimitiveOrBoxed(Class<?> cls){
        return cls == Boolean.class || cls == Boolean.TYPE
                || cls == Byte.class || cls == Byte.TYPE
                || cls == Short.class || cls == Short.TYPE
                || cls == Integer.class || cls == Integer.TYPE
                || cls == Long.class || cls == Long.TYPE
                || cls == Float.class || cls == Float.TYPE
                || cls == Double.class || cls == Double.TYPE
                || cls == Character.class || cls == Character.TYPE;
    }

}
