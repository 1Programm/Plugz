package com.programm.projects.plugz.simple.webserv.entityutil;

import com.programm.projects.plugz.magic.api.db.Ignore;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static com.programm.projects.plugz.simple.webserv.entityutil.EntityUtils.isNotSameClass;
import static com.programm.projects.plugz.simple.webserv.entityutil.EntityUtils.nameToStd;

public class EntityMapper {

    @RequiredArgsConstructor
    private static class FieldFieldGetter implements IFieldGetter {

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
    private static class MethodFieldGetter implements IFieldGetter {

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
    private static class FieldFieldSetter implements IFieldSetter {

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
    private static class MethodFieldSetter implements IFieldSetter {

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

    public static EntityEntry createEntry(Class<?> cls) throws EntityMapException {
        int clsModifiers = cls.getModifiers();

        if(Modifier.isPrivate(clsModifiers)) throw new EntityMapException("Class is private and cannot be mapped!");

        Map<String, FieldEntry> entries = new HashMap<>();

        Map<String, Class<?>> nameTypeMap = new HashMap<>();
        Map<String, Boolean> ignoreMap = new HashMap<>();

        Field[] fields = cls.getDeclaredFields();
        for(Field field : fields){
            String fName = field.getName();
            String stdName = nameToStd(fName);

            if(field.isAnnotationPresent(Ignore.class)) {
                ignoreMap.put(stdName, true);
                continue;
            }

            Class<?> type = field.getType();
            int mods = field.getModifiers();

            if(!Modifier.isStatic(mods)) {
                boolean accessible = !Modifier.isPrivate(mods);

                if (!Modifier.isFinal(mods)) {
                    entries.computeIfAbsent(stdName, n -> new FieldEntry(type, field)).setter = new FieldFieldSetter(field, accessible);
                }

                nameTypeMap.put(stdName, type);
                entries.computeIfAbsent(stdName, n -> new FieldEntry(type, field)).getter = new FieldFieldGetter(field, accessible);
            }
        }

        Method[] methods = cls.getDeclaredMethods();
        for(Method method : methods){
            if(method.isAnnotationPresent(Ignore.class)) continue;

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

                if(type != null && isNotSameClass(type, getType)) throw new EntityMapException("Getter for field [" + stdName + "] and previously found type do not match! (" + getType + " <-> " + type + ")");


                nameTypeMap.put(stdName, getType);
                entries.computeIfAbsent(stdName, n -> new FieldEntry(type, null)).getter = new MethodFieldGetter(method, accessible);
            }
            else if(stdName.startsWith("set_")){
                stdName = stdName.substring("set_".length());

                if(ignoreMap.containsKey(stdName)) continue;

                if(method.getParameterCount() != 1) throw new EntityMapException("Invalid setter should accept 1 parameter! (" + method + ")");

                Class<?> setType = method.getParameterTypes()[0];
                Class<?> type = nameTypeMap.get(stdName);

                if(type != null && isNotSameClass(type, setType)) throw new EntityMapException("Setter for field [" + stdName + "] and previously found type do not match! (" + setType + " <-> " + type + ")");

                nameTypeMap.put(stdName, setType);
                entries.computeIfAbsent(stdName, n -> new FieldEntry(type, null)).setter = new MethodFieldSetter(method, accessible);
            }
        }

        return new EntityEntry(cls, entries, clsModifiers);
    }
}
