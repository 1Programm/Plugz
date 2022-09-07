package com.programm.plugz.cls.analyzer;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;

public class ClassAnalyzer {

    private static final AnalyzedParameterizedType OBJECT_TYPE = new AnalyzedParameterizedType(Object.class, null, Collections.emptyMap());

    private static AnalyzedParameterizedType getTypeOrObject(AnalyzedParameterizedType type){
        return type == null ? OBJECT_TYPE : type;
    }

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
        boolean isAllCaps = true;

        /*
        value           -> value
        testValue       -> test_value
        theBigValue     -> the_big_value
        myAValue        -> my_a_value
        CAPS_VALUE_TEST -> caps_value_test
         */

        for(int i=0;i<s.length();i++){
            char c = s.charAt(i);

            if(c == '_'){
                sb.append(c);
            }
            else {
                boolean isUpper = Character.isUpperCase(c);

                if(!isAllCaps && isUpper){
                    sb.append("_");
                }

                if(isUpper){
                    sb.append(Character.toLowerCase(c));
                }
                else {
                    sb.append(c);
                    isAllCaps = false;
                }
            }
        }

        return sb.toString();
    }

    private static String getBeanStringForAnalyzedClass(Class<?> cls, Map<String, AnalyzedParameterizedType> genericTypes){
        if(genericTypes == null || genericTypes.isEmpty()){
            return cls.toString();
        }
        else {
            return cls.toString() + genericTypes;
        }
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






    private final boolean doCaching;
    private final boolean generalizeFieldNames;
    private final boolean deepAnalyze;
    private final boolean deepProperties;

    private final Map<String, AnalyzedPropertyClass> cachedClasses;
    private final Map<String, AnalyzedParameterizedType> cachedAnalyzedParameterizedTypeMap = new HashMap<>();

    private final List<Predicate<Field>> ignorePropertyFields = new ArrayList<>();
    private final List<Predicate<Method>> ignorePropertyMethods = new ArrayList<>();


    public ClassAnalyzer(boolean doCaching, boolean generalizeFieldNames, boolean deepAnalyze, boolean deepProperties) {
        this.doCaching = doCaching;
        this.generalizeFieldNames = generalizeFieldNames;
        this.deepAnalyze = deepAnalyze;
        this.deepProperties = deepProperties;

        cachedClasses = doCaching ? new HashMap<>() : null;
    }

    public ClassAnalyzer ignorePropertyField(Predicate<Field> pred){
        ignorePropertyFields.add(pred);
        return this;
    }

    public ClassAnalyzer ignorePropertyMethod(Predicate<Method> pred){
        ignorePropertyMethods.add(pred);
        return this;
    }

    public AnalyzedPropertyClass analyzeProperty(Class<?> cls) throws ClassAnalyzeException {
        return analyzeProperty(cls, Collections.emptyMap());
    }

    private AnalyzedPropertyClass analyzeProperty(Class<?> cls, Type genericType, Map<String, AnalyzedParameterizedType> genericTypes) throws ClassAnalyzeException {
        AnalyzedParameterizedType analyzedClassType = analyzeParameterizedType(cls, genericType, genericTypes);
        return analyzeProperty(analyzedClassType, cls);
    }

    public AnalyzedPropertyClass analyzeProperty(Class<?> cls, Map<String, AnalyzedParameterizedType> genericTypes) throws ClassAnalyzeException {
        AnalyzedParameterizedType analyzedClassType = analyzeParameterizedCls(cls, genericTypes);
        return analyzeProperty(analyzedClassType, cls);
    }

    public AnalyzedPropertyClass analyzeProperty(AnalyzedParameterizedType analyzedClassType, Class<?> cls) throws ClassAnalyzeException {
        String beanString = analyzedClassType.toString();

        if(doCaching){
            AnalyzedPropertyClass analyzedClass = cachedClasses.get(beanString);
            if(analyzedClass != null) return analyzedClass;
        }

        int clsModifiers = cls.getModifiers();
        if(Modifier.isPrivate(clsModifiers)) throw new ClassAnalyzeException("Class is private and cannot be analyzed!");

        Map<String, PropertyEntry> entries = new HashMap<>();

        Map<String, Class<?>> nameTypeMap = new HashMap<>();
        Map<String, Boolean> ignoreMap = new HashMap<>();

        Field[] fields = cls.getDeclaredFields();
        for(Field field : fields){
            String fName = field.getName();
            if(fName.startsWith("this$")) continue;

            String stdName = generalizeFieldNames ? nameToStd(fName) : fName;

            if(testIgnore(field, ignorePropertyFields)) {
                ignoreMap.put(stdName, true);
                continue;
            }

            Class<?> classType = field.getType();
            Type genericType = field.getGenericType();
            AnalyzedPropertyClass analyzedParameterizedPropertyClass;
            if(deepProperties){
                analyzedParameterizedPropertyClass = analyzeProperty(classType, genericType, analyzedClassType.getParameterizedTypeMap());
            }
            else {
                AnalyzedParameterizedType analyzedParameterizedType = analyzeParameterizedType(classType, genericType, analyzedClassType.getParameterizedTypeMap());
                analyzedParameterizedPropertyClass = new AnalyzedPropertyClass(analyzedParameterizedType, Collections.emptyMap());
            }


            int mods = field.getModifiers();

            if(!Modifier.isStatic(mods)) {
                boolean accessible = Modifier.isPublic(clsModifiers) && Modifier.isPublic(mods);

                if (!Modifier.isFinal(mods)) {
                    entries.computeIfAbsent(stdName, n -> new PropertyEntry(analyzedParameterizedPropertyClass, field)).setter = new FieldPropertySetter(field, accessible);
                }

                nameTypeMap.put(stdName, analyzedParameterizedPropertyClass.getType());
                entries.computeIfAbsent(stdName, n -> new PropertyEntry(analyzedParameterizedPropertyClass, field)).getter = new FieldPropertyGetter(field, accessible);
            }
        }

        Method[] methods = cls.getDeclaredMethods();
        for(Method method : methods){
            if(testIgnore(method, ignorePropertyMethods)) continue;

            String mName = method.getName();
            String stdName = generalizeFieldNames ? nameToStd(mName) : mName;

            int mods = method.getModifiers();

            if(Modifier.isStatic(mods)) continue;
            boolean accessible = Modifier.isPublic(clsModifiers) && Modifier.isPublic(mods);

            if(stdName.startsWith("get_") || stdName.startsWith("is_")){
                if(stdName.charAt(2) == '_'){
                    stdName = stdName.substring("is_".length());
                }
                else {
                    stdName = stdName.substring("get_".length());
                }

                if(ignoreMap.containsKey(stdName)) continue;

                Class<?> classType = method.getReturnType();
                Class<?> oldType = nameTypeMap.get(stdName);

                if(oldType != null && isNotSameClass(classType, oldType)) throw new ClassAnalyzeException("Getter Method for property [" + stdName + "] and previously found type do not match! (" + classType + " <-> " + oldType + ")");

                Type genericType = method.getGenericReturnType();
                AnalyzedPropertyClass analyzedParameterizedPropertyClass;
                if(deepProperties){
                    analyzedParameterizedPropertyClass = analyzeProperty(classType, genericType, analyzedClassType.getParameterizedTypeMap());
                }
                else {
                    AnalyzedParameterizedType analyzedParameterizedType = analyzeParameterizedType(classType, genericType, analyzedClassType.getParameterizedTypeMap()/*genericTypes*/);
                    analyzedParameterizedPropertyClass = new AnalyzedPropertyClass(analyzedParameterizedType, Collections.emptyMap());
                }

                nameTypeMap.put(stdName, analyzedParameterizedPropertyClass.getType());
                entries.computeIfAbsent(stdName, n -> new PropertyEntry(analyzedParameterizedPropertyClass, null)).getter = new MethodPropertyGetter(method, accessible);
            }
            else if(stdName.startsWith("set_")){
                stdName = stdName.substring("set_".length());

                if(ignoreMap.containsKey(stdName)) continue;

                if(method.getParameterCount() != 1) throw new ClassAnalyzeException("Invalid setter should accept only 1 parameter! (" + method + ")");

                Class<?> classType = method.getParameterTypes()[0];
                Class<?> oldType = nameTypeMap.get(stdName);

                if(oldType != null && isNotSameClass(classType, oldType)) throw new ClassAnalyzeException("Setter Method for property [" + stdName + "] and previously found type do not match! (" + classType + " <-> " + oldType + ")");

                Type genericType = method.getGenericParameterTypes()[0];
                AnalyzedPropertyClass analyzedParameterizedPropertyClass;
                if(deepProperties){
                    analyzedParameterizedPropertyClass = analyzeProperty(classType, genericType, analyzedClassType.getParameterizedTypeMap());
                }
                else {
                    AnalyzedParameterizedType analyzedParameterizedType = analyzeParameterizedType(classType, genericType, analyzedClassType.getParameterizedTypeMap()/*genericTypes*/);
                    analyzedParameterizedPropertyClass = new AnalyzedPropertyClass(analyzedParameterizedType, Collections.emptyMap());
                }

                nameTypeMap.put(stdName, analyzedParameterizedPropertyClass.getType());
                entries.computeIfAbsent(stdName, n -> new PropertyEntry(analyzedParameterizedPropertyClass, null)).setter = new MethodPropertySetter(method, accessible);
            }
        }


        AnalyzedPropertyClass analyzedClass = new AnalyzedPropertyClass(analyzedClassType, entries);

        if(doCaching){
            cachedClasses.put(beanString, analyzedClass);
        }

        return analyzedClass;
    }

    public AnalyzedParameterizedType analyzeParameterizedCls(Class<?> cls) throws ClassAnalyzeException {
        return analyzeParameterizedCls(cls, null);
    }

    public AnalyzedParameterizedType analyzeParameterizedCls(Class<?> cls, Map<String, AnalyzedParameterizedType> genericTypes) throws ClassAnalyzeException {
        Map<String, AnalyzedParameterizedType> genericTypesMap = null;

        TypeVariable<?>[] typeVariables = cls.getTypeParameters();
        for (TypeVariable<?> typeVariable : typeVariables) {
            if(genericTypesMap == null) genericTypesMap = new HashMap<>();
            String typeName = typeVariable.getName();

            AnalyzedParameterizedType genericType = null;
            if(genericTypes != null) genericType = genericTypes.get(typeName);
            if(genericType == null) genericType = OBJECT_TYPE;

            genericTypesMap.put(typeName, genericType);
        }

        String beanString = getBeanStringForAnalyzedClass(cls, genericTypesMap);

        if(cachedAnalyzedParameterizedTypeMap.containsKey(beanString)){
            return cachedAnalyzedParameterizedTypeMap.get(beanString);
        }

        Class<?> parentCls = cls.getSuperclass();
        Type genericParentCls = cls.getGenericSuperclass();

        AnalyzedParameterizedType analyzedParent = null;
        if(deepAnalyze) analyzedParent = analyzeParameterizedType(parentCls, genericParentCls, genericTypesMap);
        return newTypeAndAddToCache(beanString, cls, analyzedParent, genericTypesMap);
    }

    public AnalyzedParameterizedType analyzeParameterizedType(Class<?> typeClass, Type genericType, Map<String, AnalyzedParameterizedType> genericTypes) throws ClassAnalyzeException {
        if(genericType instanceof ParameterizedType parameterizedType){
            Class<?> theType = (Class<?>) parameterizedType.getRawType();
            Map<String, AnalyzedParameterizedType> genericParentTypes = createParameterizedTypeMap(theType, parameterizedType, genericTypes);
            return analyzeParameterizedCls(theType, genericParentTypes);
        }
        else if(genericType instanceof TypeVariable typeVariable){
            String typeVariableName = typeVariable.getName();
            if(genericTypes == null) throw new ClassAnalyzeException("No generic info [" + typeVariableName + "] provided for class [" + typeClass + "]!");

            AnalyzedParameterizedType analyzedType = genericTypes.get(typeVariableName);
            if(analyzedType == null) throw new ClassAnalyzeException("No generic info [" + typeVariableName + "] provided for class [" + typeClass + "]!");

            return analyzedType;
        }
        else if(typeClass == null || typeClass == Object.class){
            return null;
        }
        else {
            String beanString = getBeanStringForAnalyzedClass(typeClass, genericTypes);

            if (cachedAnalyzedParameterizedTypeMap.containsKey(beanString)) {
                return cachedAnalyzedParameterizedTypeMap.get(beanString);
            }

            Class<?> parentCls = typeClass.getSuperclass();
            Type genericParentCls = typeClass.getGenericSuperclass();

            AnalyzedParameterizedType analyzedParent = null;
            if(deepAnalyze) analyzedParent = analyzeParameterizedType(parentCls, genericParentCls, Collections.emptyMap());
            return newTypeAndAddToCache(beanString, typeClass, analyzedParent, Collections.emptyMap());
        }
    }

    private Map<String, AnalyzedParameterizedType> createParameterizedTypeMap(Class<?> typeClass, ParameterizedType parameterizedType, Map<String, AnalyzedParameterizedType> genericTypes) throws ClassAnalyzeException {
        Class<?> theType = (Class<?>) parameterizedType.getRawType();
        TypeVariable<?>[] parentTypeNames = theType.getTypeParameters();
        Type[] parentTypes = parameterizedType.getActualTypeArguments();

        Map<String, AnalyzedParameterizedType> genericParentTypes = new HashMap<>();
        for(int i=0;i<parentTypes.length;i++){
            Type parentType = parentTypes[i];
            String parentTypeName = parentTypeNames[i].getName();

            if(parentType instanceof TypeVariable typeVariable){
                String typeVariableName = typeVariable.getName();
                if(genericTypes == null) throw new ClassAnalyzeException("No generic info [" + typeVariableName + "] provided for class [" + typeClass + "]!");

                AnalyzedParameterizedType analyzedParentType = genericTypes.get(typeVariableName);
                if(analyzedParentType == null) throw new ClassAnalyzeException("No generic info [" + typeVariableName + "] provided for class [" + typeClass + "]!");

                genericParentTypes.put(parentTypeName, analyzedParentType);
            }
            else if(parentType instanceof ParameterizedType parameterizedParentType){
                Class<?> theParentType = (Class<?>) parameterizedParentType.getRawType();
                Map<String, AnalyzedParameterizedType> genericParentTypesMap = createParameterizedTypeMap(theParentType, parameterizedParentType, genericTypes);
                AnalyzedParameterizedType analyzedParentType = analyzeParameterizedCls(theParentType, genericParentTypesMap);
                genericParentTypes.put(parentTypeName, analyzedParentType);
            }
            else {
                Class<?> parentSubTypeClass = (Class<?>) parentType;
                AnalyzedParameterizedType analyzedParentType = analyzeParameterizedType(parentSubTypeClass, parentType, genericTypes);
                genericParentTypes.put(parentTypeName, analyzedParentType);
            }
        }

        return genericParentTypes;
    }

    private AnalyzedParameterizedType newTypeAndAddToCache(String beanString, Class<?> cls, AnalyzedParameterizedType parent, Map<String, AnalyzedParameterizedType> genericTypes) {
        if(genericTypes == null) genericTypes = Collections.emptyMap();
        AnalyzedParameterizedType newType = new AnalyzedParameterizedType(cls, parent, genericTypes);
        cachedAnalyzedParameterizedTypeMap.put(beanString, newType);
        return newType;
    }

}
