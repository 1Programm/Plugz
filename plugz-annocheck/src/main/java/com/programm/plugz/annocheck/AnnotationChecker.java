package com.programm.plugz.annocheck;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Queue;

public class AnnotationChecker {

    public static void checkAllDeclared(Class<?> cls) throws AnnotationCheckException {
        checkClass(cls);

        Constructor<?>[] declaredConstructors = cls.getDeclaredConstructors();
        for(Constructor<?> constructor : declaredConstructors){
            checkConstructor(constructor);
        }

        Field[] declaredFields = cls.getDeclaredFields();
        for(Field field : declaredFields){
            checkField(field);
        }

        Method[] declaredMethods = cls.getDeclaredMethods();
        for(Method method : declaredMethods){
            checkMethod(method);
        }
    }

    public static void checkClass(Class<?> cls) throws AnnotationCheckException {
        try {
            Annotation[] declaredAnnotations = cls.getDeclaredAnnotations();

            for (Annotation annotation : declaredAnnotations) {
                Class<? extends Annotation> annotationCls = annotation.annotationType();

                Check singleCheck = annotationCls.getAnnotation(Check.class);
                if (singleCheck != null) {
                    checkClass(cls, annotationCls, declaredAnnotations, singleCheck);
                }

                Checks multiCheck = annotationCls.getAnnotation(Checks.class);
                if (multiCheck != null) {
                    for(Check check : multiCheck.value()) {
                        checkClass(cls, annotationCls, declaredAnnotations, check);
                    }
                }
            }
        }
        catch (AnnotationCheckException e){
            throw new AnnotationCheckException("Check failed for class: [" + cls.getName() + "]!", e);
        }
    }

    public static void checkConstructor(Constructor<?> constructor) throws AnnotationCheckException {
        try {
            Class<?> declaringCls = constructor.getDeclaringClass();
            Annotation[] clsDeclaredAnnotations = declaringCls.getDeclaredAnnotations();
            Annotation[] declaredAnnotations = constructor.getDeclaredAnnotations();

            for (Annotation annotation : declaredAnnotations) {
                Class<? extends Annotation> annotationCls = annotation.annotationType();

                Check singleCheck = annotationCls.getAnnotation(Check.class);
                if (singleCheck != null) {
                    checkType(declaringCls, annotationCls, clsDeclaredAnnotations, declaredAnnotations, singleCheck, ElementType.CONSTRUCTOR, "Constructors");
                }

                Checks multiCheck = annotationCls.getAnnotation(Checks.class);
                if (multiCheck != null) {
                    for(Check check : multiCheck.value()) {
                        checkType(declaringCls, annotationCls, clsDeclaredAnnotations, declaredAnnotations, check, ElementType.CONSTRUCTOR, "Constructors");
                    }
                }
            }
        }
        catch (AnnotationCheckException e){
            throw new AnnotationCheckException("Check failed for constructor: [" + constructor + "]!", e);
        }
    }

    public static void checkField(Field field) throws AnnotationCheckException {
        try {
            Class<?> declaringCls = field.getDeclaringClass();
            Annotation[] clsDeclaredAnnotations = declaringCls.getDeclaredAnnotations();
            Annotation[] declaredAnnotations = field.getDeclaredAnnotations();

            for (Annotation annotation : declaredAnnotations) {
                Class<? extends Annotation> annotationCls = annotation.annotationType();

                Check singleCheck = annotationCls.getAnnotation(Check.class);
                if (singleCheck != null) {
                    checkType(declaringCls, annotationCls, clsDeclaredAnnotations, declaredAnnotations, singleCheck, ElementType.FIELD, "Fields");
                }

                Checks multiCheck = annotationCls.getAnnotation(Checks.class);
                if (multiCheck != null) {
                    for(Check check : multiCheck.value()) {
                        checkType(declaringCls, annotationCls, clsDeclaredAnnotations, declaredAnnotations, check, ElementType.FIELD, "Fields");
                    }
                }
            }
        }
        catch (AnnotationCheckException e){
            throw new AnnotationCheckException("Check failed for field: [" + field + "]!", e);
        }
    }

    public static void checkMethod(Method method) throws AnnotationCheckException {
        try {
            Class<?> declaringCls = method.getDeclaringClass();
            Annotation[] clsDeclaredAnnotations = declaringCls.getDeclaredAnnotations();
            Annotation[] declaredAnnotations = method.getDeclaredAnnotations();

            for (Annotation annotation : declaredAnnotations) {
                Class<? extends Annotation> annotationCls = annotation.annotationType();

                Check singleCheck = annotationCls.getAnnotation(Check.class);
                if (singleCheck != null) {
                    checkType(declaringCls, annotationCls, clsDeclaredAnnotations, declaredAnnotations, singleCheck, ElementType.METHOD, "Methods");
                }

                Checks multiCheck = annotationCls.getAnnotation(Checks.class);
                if (multiCheck != null) {
                    for(Check check : multiCheck.value()) {
                        checkType(declaringCls, annotationCls, clsDeclaredAnnotations, declaredAnnotations, check, ElementType.METHOD, "Methods");
                    }
                }
            }
        }
        catch (AnnotationCheckException e){
            throw new AnnotationCheckException("Check failed for method: [" + method + "]!", e);
        }
    }










    private static void checkClass(Class<?> cls, Class<? extends Annotation> selectedAnnotation, Annotation[] declaredAnnotations, Check check) throws AnnotationCheckException {
        if(invalidTargetType(check, ElementType.TYPE)) return;

        if(!checkOrAnnotated(check.clsAnnotatedWith(), declaredAnnotations, true)){
            throw new AnnotationCheckException("Classes annotated with [" + selectedAnnotation.getName() + "] MUST also be annotated with " + orString(check.clsAnnotatedWith()) + "!");
        }

        if(!checkOrAnnotated(check.annotatedWith(), declaredAnnotations, true)){
            throw new AnnotationCheckException("Classes annotated with [" + selectedAnnotation.getName() + "] MUST also be annotated with " + orString(check.annotatedWith()) + "!");
        }

        if(checkOrAnnotated(check.clsNotAnnotatedWith(), declaredAnnotations, false)){
            throw new AnnotationCheckException("Classes annotated with [" + selectedAnnotation.getName() + "] MUST NOT be annotated with " + orString(check.clsNotAnnotatedWith()) + "!");
        }

        if(checkOrAnnotated(check.notAnnotatedWith(), declaredAnnotations, false)){
            throw new AnnotationCheckException("Classes annotated with [" + selectedAnnotation.getName() + "] MUST NOT be annotated with " + orString(check.notAnnotatedWith()) + "!");
        }

        if(!checkOrImplementing(check.clsImplementing(), cls, true)){
            throw new AnnotationCheckException("Classes annotated with [" + selectedAnnotation.getName() + "] MUST implement " + orString(check.clsImplementing()) + "!");
        }

        if(checkOrImplementing(check.clsNotImplementing(), cls, false)){
            throw new AnnotationCheckException("Classes annotated with [" + selectedAnnotation.getName() + "] MUST NOT implement " + orString(check.clsNotImplementing()) + "!");
        }
    }

    private static void checkType(Class<?> declaringCls, Class<? extends Annotation> selectedAnnotation, Annotation[] clsDeclaredAnnotations, Annotation[] declaredAnnotations, Check check, ElementType type, String name) throws AnnotationCheckException {
        if(invalidTargetType(check, type)) return;

        if(!checkOrAnnotated(check.clsAnnotatedWith(), clsDeclaredAnnotations, true)){
            throw new AnnotationCheckException(name + " annotated with [" + selectedAnnotation.getName() + "] MUST be inside a class annotated with " + orString(check.clsAnnotatedWith()) + "!");
        }

        if(!checkOrAnnotated(check.annotatedWith(), declaredAnnotations, true)){
            throw new AnnotationCheckException(name + " annotated with [" + selectedAnnotation.getName() + "] MUST also be annotated with " + orString(check.annotatedWith()) + "!");
        }

        if(checkOrAnnotated(check.clsNotAnnotatedWith(), clsDeclaredAnnotations, false)){
            throw new AnnotationCheckException(name + " annotated with [" + selectedAnnotation.getName() + "] MUST NOT be inside a class annotated with " + orString(check.clsNotAnnotatedWith()) + "!");
        }

        if(checkOrAnnotated(check.notAnnotatedWith(), declaredAnnotations, false)){
            throw new AnnotationCheckException(name + " annotated with [" + selectedAnnotation.getName() + "] MUST NOT be annotated with " + orString(check.notAnnotatedWith()) + "!");
        }

        if(!checkOrImplementing(check.clsImplementing(), declaringCls, true)){
            throw new AnnotationCheckException(name + " annotated with [" + selectedAnnotation.getName() + "] MUST be inside a class which implements " + orString(check.clsImplementing()) + "!");
        }

        if(checkOrImplementing(check.clsNotImplementing(), declaringCls, false)){
            throw new AnnotationCheckException(name + " annotated with [" + selectedAnnotation.getName() + "] MUST NOT be inside a class which implements " + orString(check.clsNotImplementing()) + "!");
        }
    }

    private static boolean checkOrAnnotated(Class<? extends Annotation>[] annotations, Annotation[] declaredAnnotations, boolean defaultValue) {
        for(Class<? extends Annotation> annotationCls : annotations){
            if(containsAnnotationCls(declaredAnnotations, annotationCls)){
                return true;
            }
        }

        return defaultValue;
    }

    private static boolean checkOrImplementing(Class<?>[] toImplement, Class<?> target, boolean defaultValue){
        for(Class<?> cls : toImplement){
            if(isImplementing(target, cls)){
                return true;
            }
        }

        return defaultValue;
    }















    private static boolean containsAnnotationCls(Annotation[] annotations, Class<? extends Annotation> mustContain){
        for(Annotation annotation : annotations){
            if(annotation.annotationType() == mustContain) return true;
        }
        return false;
    }

    private static boolean isImplementing(Class<?> cls, Class<?> toImplement){
        Queue<Class<?>> queue = new ArrayDeque<>();
        queue.add(cls);

        while(!queue.isEmpty()){
            Class<?> cur = queue.poll();

            Class<?>[] interfaceClasses = cur.getInterfaces();
            for(Class<?> i : interfaceClasses){
                if(i == toImplement) return true;
                queue.add(i);
            }

            Class<?> superClass = cur.getSuperclass();
            if(superClass != null){
                if(superClass == toImplement) return true;
                queue.add(superClass);
            }
        }

        return false;
    }

    private static boolean invalidTargetType(Check check, ElementType type){
        if(check.target().length == 0) return false;

        for(ElementType t : check.target()){
            if(type == t) return false;
        }
        return true;
    }

    private static String orString(Class<?>[] classes){
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for(Class<?> cls : classes){
            if(sb.length() != 1) sb.append("] OR [");
            sb.append(cls.getName());
        }

        sb.append("]");

        return sb.toString();
    }

}
