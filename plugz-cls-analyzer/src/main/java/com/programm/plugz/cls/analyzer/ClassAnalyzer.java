package com.programm.plugz.cls.analyzer;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ClassAnalyzer {

    @RequiredArgsConstructor
    private static class FieldPropertyGetter implements IClassPropertyGetter {

        private final Field field;
        private final boolean accessible;

        @Override
        public Object get(Object instance) {
            if(!accessible) field.setAccessible(true);

            Object result;
            try {
                result = field.get(instance);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("INVALID STATE: Should have been set to accessible!", e);
            }

            if(!accessible) field.setAccessible(false);

            return result;
        }

        @Override
        public int modifiers() {
            return field.getModifiers();
        }
    }

    @RequiredArgsConstructor
    private static class MethodPropertyGetter implements IClassPropertyGetter {

        private final Method method;
        private final boolean accessible;

        @Override
        public Object get(Object instance) throws InvocationTargetException {
            if(!accessible) method.setAccessible(true);

            Object result;
            try {
                result = method.invoke(instance);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("INVALID STATE: Should have been set to accessible!", e);
            }

            if(!accessible) method.setAccessible(false);

            return result;
        }

        @Override
        public int modifiers() {
            return method.getModifiers();
        }
    }

    @RequiredArgsConstructor
    private static class FieldPropertySetter implements IClassPropertySetter {

        private final Field field;
        private final boolean accessible;

        @Override
        public void set(Object instance, Object value) {
            if(!accessible) field.setAccessible(true);

            try {
                field.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("INVALID STATE: Should have been set to accessible!", e);
            }

            if(!accessible) field.setAccessible(false);
        }

        @Override
        public int modifiers() {
            return field.getModifiers();
        }
    }

    @RequiredArgsConstructor
    private static class MethodPropertySetter implements IClassPropertySetter {

        private final Method method;
        private final boolean accessible;

        @Override
        public void set(Object instance, Object value) throws InvocationTargetException {
            if(!accessible) method.setAccessible(true);

            try {
                method.invoke(instance, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("INVALID STATE: Should have been set to accessible!", e);
            }

            if(!accessible) method.setAccessible(false);
        }

        @Override
        public int modifiers() {
            return method.getModifiers();
        }
    }

    private static String nameToStd(String s){
        StringBuilder sb = new StringBuilder();

        for(int i=0;i<s.length();i++){
            char c = s.charAt(i);

            if(Character.isUpperCase(c)){
                sb.append("_").append(Character.toLowerCase(c));
            }
            else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private static boolean isNotSameClass(Class<?> a, Class<?> b){
        if(a == b) return false;

        if(a.isPrimitive()){
            return a != toPrimitive(b);
        }

        if(b.isPrimitive()){
            return b != toPrimitive(a);
        }

        return true;
    }

    private static Class<?> toPrimitive(Class<?> c){
        try {
            return (Class<?>) c.getField("TYPE").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

    private static <T> boolean testIgnore(T value, List<Predicate<T>> predicates){
        for(int i=0;i<predicates.size();i++){
            if(predicates.get(i).test(value)) return true;
        }

        return false;
    }







    private final List<Predicate<Field>> ignorePropertyFields = new ArrayList<>();
    private final List<Predicate<Method>> ignorePropertyMethods = new ArrayList<>();

    public void ignorePropertyField(Predicate<Field> pred){
        ignorePropertyFields.add(pred);
    }

    public void ignorePropertyMethod(Predicate<Method> pred){
        ignorePropertyMethods.add(pred);
    }




    public AnalyzedPropertyClass analyzeProperty(Class<?> cls) throws ClassAnalyzeException {
        int clsModifiers = cls.getModifiers();
        if(Modifier.isPrivate(clsModifiers)) throw new ClassAnalyzeException("Class is private and cannot be analyzed!");

        Map<String, PropertyEntry> entries = new HashMap<>();

        Map<String, Class<?>> nameTypeMap = new HashMap<>();
        Map<String, Boolean> ignoreMap = new HashMap<>();

        Field[] fields = cls.getDeclaredFields();
        for(Field field : fields){
            String fName = field.getName();
            String stdName = nameToStd(fName);

            if(testIgnore(field, ignorePropertyFields)) {
                ignoreMap.put(stdName, true);
                continue;
            }

            Class<?> type = field.getType();
            int mods = field.getModifiers();

            if(!Modifier.isStatic(mods)) {
                boolean accessible = !Modifier.isPrivate(mods);

                if (!Modifier.isFinal(mods)) {
                    entries.computeIfAbsent(stdName, n -> new PropertyEntry(type, field)).setter = new FieldPropertySetter(field, accessible);
                }

                nameTypeMap.put(stdName, type);
                entries.computeIfAbsent(stdName, n -> new PropertyEntry(type, field)).getter = new FieldPropertyGetter(field, accessible);
            }
        }

        Method[] methods = cls.getDeclaredMethods();
        for(Method method : methods){
            if(testIgnore(method, ignorePropertyMethods)) continue;

            String mName = method.getName();
            String stdName = nameToStd(mName);

            int mods = method.getModifiers();

            if(Modifier.isStatic(mods)) continue;

            boolean accessible = !Modifier.isPrivate(mods);

            if(stdName.startsWith("get_") || stdName.startsWith("is_")){
                if(stdName.charAt(2) == '_'){
                    stdName = stdName.substring("is_".length());
                }
                else {
                    stdName = stdName.substring("get_".length());
                }

                if(ignoreMap.containsKey(stdName)) continue;

                Class<?> getType = method.getReturnType();
                Class<?> type = nameTypeMap.get(stdName);

                if(type != null && isNotSameClass(type, getType)) throw new ClassAnalyzeException("Getter Method for property [" + stdName + "] and previously found type do not match! (" + getType + " <-> " + type + ")");


                nameTypeMap.put(stdName, getType);
                entries.computeIfAbsent(stdName, n -> new PropertyEntry(type, null)).getter = new MethodPropertyGetter(method, accessible);
            }
            else if(stdName.startsWith("set_")){
                stdName = stdName.substring("set_".length());

                if(ignoreMap.containsKey(stdName)) continue;

                if(method.getParameterCount() != 1) throw new ClassAnalyzeException("Invalid setter should accept only 1 parameter! (" + method + ")");

                Class<?> setType = method.getParameterTypes()[0];
                Class<?> type = nameTypeMap.get(stdName);

                if(type != null && isNotSameClass(type, setType)) throw new ClassAnalyzeException("Setter Method for property [" + stdName + "] and previously found type do not match! (" + setType + " <-> " + type + ")");

                nameTypeMap.put(stdName, setType);
                entries.computeIfAbsent(stdName, n -> new PropertyEntry(type, null)).setter = new MethodPropertySetter(method, accessible);
            }
        }

        return new AnalyzedPropertyClass(cls, entries, clsModifiers);
    }

}
