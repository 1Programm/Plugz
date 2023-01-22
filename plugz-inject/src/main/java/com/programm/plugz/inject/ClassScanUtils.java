package com.programm.plugz.inject;

import java.lang.annotation.Annotation;

public class ClassScanUtils {

    public static boolean implementsClass(Class<?> cls, Class<?> toImplement){
        return implementsClass(cls, toImplement, -1);
    }

    public static boolean implementsClassFlat(Class<?> cls, Class<?> toImplement){
        return implementsClass(cls, toImplement, 0);
    }

    public static boolean implementsClass(Class<?> cls, Class<?> toImplement, int generations){
        if(cls == null || cls == Object.class) return false;

        Class<?>[] interfaces = cls.getInterfaces();
        for(Class<?> i : interfaces){
            if(i == toImplement) return true;

            if(generations != 0){
                if(implementsClass(i, toImplement, generations - 1)) return true;
            }
        }

        Class<?> superCls = cls.getSuperclass();
        if(superCls == toImplement) return true;

        if(generations != 0){
            return implementsClass(superCls, toImplement, generations - 1);
        }

        return false;
    }

    public static boolean annotatedWithFlat(Class<?> cls, Class<? extends Annotation> annotatedBy){
        return annotatedWith(cls, annotatedBy, false, false, 0);
    }

    public static boolean annotatedWith(Class<?> cls, Class<? extends Annotation> annotatedBy, boolean searchInInterfaces, boolean searchInSuperclass){
        return annotatedWith(cls, annotatedBy, searchInInterfaces, searchInSuperclass, -1);
    }

    public static boolean annotatedWith(Class<?> cls, Class<? extends Annotation> annotatedBy, boolean searchInInterfaces, boolean searchInSuperclass, int generations){
        if(cls == null || cls == Object.class) return false;
        if(cls.isAnnotationPresent(annotatedBy)) return true;
        if(generations == 0) return false;

        if(searchInInterfaces) {
            Class<?>[] interfaces = cls.getInterfaces();
            for(Class<?> i : interfaces){
                if(annotatedWith(i, annotatedBy, searchInInterfaces, searchInSuperclass, generations - 1)){
                    return true;
                }
            }
        }

        if(searchInSuperclass) {
            Class<?> superCls = cls.getSuperclass();
            return annotatedWith(superCls, annotatedBy, searchInInterfaces, searchInSuperclass, generations - 1);
        }

        return false;
    }

}
