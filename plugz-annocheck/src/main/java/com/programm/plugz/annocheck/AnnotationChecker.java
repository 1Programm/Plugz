package com.programm.plugz.annocheck;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotationChecker {

    public interface ICondition {
        ICondition set(Class<?> cls);
        ICondition and(Class<?> cls);
        ICondition or(Class<?> cls);
        void seal();
    }

    private static class ConditionConfigGroup implements ICondition {
        private final List<ConditionConfig> configs;

        public ConditionConfigGroup(List<ConditionConfig> configs) {
            this.configs = configs;
        }

        @Override
        public ICondition set(Class<?> cls) {
            configs.forEach(c -> c.set(cls));
            return this;
        }

        @Override
        public ICondition and(Class<?> cls) {
            configs.forEach(c -> c.and(cls));
            return this;
        }

        @Override
        public ICondition or(Class<?> cls) {
            configs.forEach(c -> c.or(cls));
            return this;
        }

        @Override
        public void seal() {
            configs.forEach(ICondition::seal);
        }
    }

    private static class ConditionConfig implements ICondition {
        private List<List<Class<?>>> orAndList;
        private boolean sealed;

        @Override
        public ConditionConfig set(Class<?> cls){
            if(sealed) throw new IllegalStateException("This configuration is sealed!");

            if(orAndList == null){
                orAndList = new ArrayList<>();
            }
            else {
                orAndList.clear();
            }

            List<Class<?>> classes = new ArrayList<>();
            classes.add(cls);
            orAndList.add(classes);

            return this;
        }

        @Override
        public ConditionConfig and(Class<?> cls){
            if(sealed) throw new IllegalStateException("This configuration is sealed!");

            if(orAndList == null) return set(cls);
            orAndList.get(orAndList.size() - 1).add(cls);
            return this;
        }

        @Override
        public ConditionConfig or(Class<?> cls){
            if(sealed) throw new IllegalStateException("This configuration is sealed!");

            List<Class<?>> newOrTerm = new ArrayList<>();
            newOrTerm.add(cls);
            orAndList.add(newOrTerm);
            return this;
        }

        @Override
        public void seal() {
            this.sealed = true;
        }
    }

    private static class AnnotationCheckConfig {
        private Map<ElementType, ConditionConfig> whitelistClassAnnotationsMap;
        private Map<ElementType, ConditionConfig> blacklistClassAnnotationsMap;
        private Map<ElementType, ConditionConfig> whitelistPartnerAnnotationsMap;
        private Map<ElementType, ConditionConfig> blacklistPartnerAnnotationsMap;
        private Map<ElementType, List<Class<?>>> whitelistContainingClassesMap;
        private Map<ElementType, List<Class<?>>> blacklistContainingClassesMap;
    }

    private final Map<Class<? extends Annotation>, AnnotationCheckConfig> annotationConfigMap = new HashMap<>();

    private ICondition helpConfig(Map<ElementType, ConditionConfig> map, ElementType... types){
        boolean forAll = (types == null || types.length == 0);

        if(forAll) {
            return map.computeIfAbsent(null, t -> new ConditionConfig());
        }
        else {
            List<ConditionConfig> groupList = new ArrayList<>();
            ConditionConfigGroup group = new ConditionConfigGroup(groupList);

            for(ElementType type : types) {
                groupList.add(map.computeIfAbsent(type, t -> new ConditionConfig()));
            }

            return group;
        }
    }

    public ICondition whitelistClassAnnotations(Class<? extends Annotation> target, ElementType... types) {
        AnnotationCheckConfig config = annotationConfigMap.computeIfAbsent(target, t -> new AnnotationCheckConfig());
        if(config.whitelistClassAnnotationsMap == null) config.whitelistClassAnnotationsMap = new HashMap<>();
        return helpConfig(config.whitelistClassAnnotationsMap, types);
    }

    public ICondition blacklistClassAnnotations(Class<? extends Annotation> target, ElementType... types) {
        AnnotationCheckConfig config = annotationConfigMap.computeIfAbsent(target, t -> new AnnotationCheckConfig());
        if(config.blacklistClassAnnotationsMap == null) config.blacklistClassAnnotationsMap = new HashMap<>();
        return helpConfig(config.blacklistClassAnnotationsMap, types);
    }

    public ICondition whitelistPartnerAnnotations(Class<? extends Annotation> target, ElementType... types) {
        AnnotationCheckConfig config = annotationConfigMap.computeIfAbsent(target, t -> new AnnotationCheckConfig());
        if(config.whitelistPartnerAnnotationsMap == null) config.whitelistPartnerAnnotationsMap = new HashMap<>();
        return helpConfig(config.whitelistPartnerAnnotationsMap, types);
    }

    public ICondition blacklistPartnerAnnotations(Class<? extends Annotation> target, ElementType... types) {
        AnnotationCheckConfig config = annotationConfigMap.computeIfAbsent(target, t -> new AnnotationCheckConfig());
        if(config.blacklistPartnerAnnotationsMap == null) config.blacklistPartnerAnnotationsMap = new HashMap<>();
        return helpConfig(config.blacklistPartnerAnnotationsMap, types);
    }

    public void whitelistContainingClass(Class<? extends Annotation> target, Class<?> cls, ElementType... types) {
        AnnotationCheckConfig config = annotationConfigMap.computeIfAbsent(target, t -> new AnnotationCheckConfig());
        if(config.whitelistContainingClassesMap == null) config.whitelistContainingClassesMap = new HashMap<>();

        if((types == null || types.length == 0)) {
            config.whitelistContainingClassesMap.computeIfAbsent(null, t -> new ArrayList<>()).add(cls);
        }
        else {
            for(ElementType type : types) {
                config.whitelistContainingClassesMap.computeIfAbsent(type, t -> new ArrayList<>()).add(cls);
            }
        }
    }

    public void blacklistContainingClass(Class<? extends Annotation> target, Class<?> cls, ElementType... types) {
        AnnotationCheckConfig config = annotationConfigMap.computeIfAbsent(target, t -> new AnnotationCheckConfig());
        if(config.blacklistContainingClassesMap == null) config.blacklistContainingClassesMap = new HashMap<>();

        if((types == null || types.length == 0)) {
            config.blacklistContainingClassesMap.computeIfAbsent(null, t -> new ArrayList<>()).add(cls);
        }
        else {
            for(ElementType type : types) {
                config.blacklistContainingClassesMap.computeIfAbsent(type, t -> new ArrayList<>()).add(cls);
            }
        }
    }





    public void checkAllDeclared(Class<?> cls) throws AnnotationCheckException {
        checkClass(cls);

        Constructor<?>[] declaredConstructors = cls.getDeclaredConstructors();
        for(Constructor<?> constructor : declaredConstructors){
            checkConstructor(cls, constructor);
        }

        Field[] declaredFields = cls.getDeclaredFields();
        for(Field field : declaredFields){
            checkField(cls, field);
        }

        Method[] declaredMethods = cls.getDeclaredMethods();
        for(Method method : declaredMethods){
            checkMethod(cls, method);
        }
    }

    public void checkClass(Class<?> cls) throws AnnotationCheckException {
        Annotation[] declaredAnnotations = cls.getDeclaredAnnotations();
        for(Annotation toCheckAnnotation : declaredAnnotations){
            Class<? extends Annotation> annotationCls = toCheckAnnotation.annotationType();

            AnnotationCheckConfig config = annotationConfigMap.get(annotationCls);
            if(config == null) continue;

            if(config.whitelistClassAnnotationsMap != null){
                if(whitelistTestForAnnotations(null, config.whitelistClassAnnotationsMap, declaredAnnotations)){
                    throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST also be annotated with " + buildOrAndString(config.whitelistClassAnnotationsMap, null));
                }

                if(whitelistTestForAnnotations(ElementType.TYPE, config.whitelistClassAnnotationsMap, declaredAnnotations)){
                    throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST also be annotated with " + buildOrAndString(config.whitelistClassAnnotationsMap, ElementType.TYPE));
                }
            }

            if(config.blacklistClassAnnotationsMap != null){
                if(blacklistTestForAnnotations(null, config.blacklistClassAnnotationsMap, declaredAnnotations)){
                    throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST NOT be annotated with " + buildOrAndString(config.blacklistClassAnnotationsMap, null));
                }

                if(blacklistTestForAnnotations(ElementType.TYPE, config.blacklistClassAnnotationsMap, declaredAnnotations)){
                    throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST NOT be annotated with " + buildOrAndString(config.blacklistClassAnnotationsMap, ElementType.TYPE));
                }
            }

            if(config.whitelistPartnerAnnotationsMap != null){
                if(whitelistTestForAnnotations(null, config.whitelistPartnerAnnotationsMap, declaredAnnotations)){
                    throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST also be annotated with " + buildOrAndString(config.whitelistPartnerAnnotationsMap, null));
                }

                if(whitelistTestForAnnotations(ElementType.TYPE, config.whitelistPartnerAnnotationsMap, declaredAnnotations)){
                    throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST also be annotated with " + buildOrAndString(config.whitelistPartnerAnnotationsMap, ElementType.TYPE));
                }
            }

            if(config.blacklistPartnerAnnotationsMap != null){
                if(blacklistTestForAnnotations(null, config.blacklistPartnerAnnotationsMap, declaredAnnotations)){
                    throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST NOT be annotated with " + buildOrAndString(config.blacklistPartnerAnnotationsMap, null));
                }

                if(blacklistTestForAnnotations(ElementType.TYPE, config.blacklistPartnerAnnotationsMap, declaredAnnotations)){
                    throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST NOT be annotated with " + buildOrAndString(config.blacklistPartnerAnnotationsMap, ElementType.TYPE));
                }
            }

            if(config.whitelistContainingClassesMap != null){
                if(whitelistTestForContainingClass(null, config.whitelistContainingClassesMap, cls)){
                    List<Class<?>> classes = config.whitelistContainingClassesMap.get(null);
                    throw new AnnotationCheckException("The [" + annotationCls.getName() + "] annotation MUST be inside one of the following classes: [" + concatList(classes, "OR") + "]");
                }

                if(whitelistTestForContainingClass(ElementType.TYPE, config.whitelistContainingClassesMap, cls)){
                    List<Class<?>> classes = config.whitelistContainingClassesMap.get(ElementType.TYPE);
                    throw new AnnotationCheckException("The [" + annotationCls.getName() + "] annotation MUST be inside one of the following classes: [" + concatList(classes, "OR") + "]");
                }
            }

            if(config.blacklistContainingClassesMap != null){
                if(blacklistTestForContainingClass(null, config.blacklistContainingClassesMap, cls)){
                    List<Class<?>> classes = config.blacklistContainingClassesMap.get(null);
                    throw new AnnotationCheckException("The [" + annotationCls.getName() + "] annotation MUST NOT be inside any of the following classes: [" + concatList(classes, "OR") + "]");
                }

                if(blacklistTestForContainingClass(ElementType.TYPE, config.blacklistContainingClassesMap, cls)){
                    List<Class<?>> classes = config.blacklistContainingClassesMap.get(ElementType.TYPE);
                    throw new AnnotationCheckException("The [" + annotationCls.getName() + "] annotation MUST NOT be inside any of the following classes: [" + concatList(classes, "OR") + "]");
                }
            }
        }
    }

    public void checkConstructor(Class<?> cls, Constructor<?> constructor) throws AnnotationCheckException {
        checkType(cls, cls.getDeclaredAnnotations(), constructor.getDeclaredAnnotations(), ElementType.CONSTRUCTOR, "Constructors");
    }

    public void checkField(Class<?> cls, Field field) throws AnnotationCheckException {
        checkType(cls, cls.getDeclaredAnnotations(), field.getDeclaredAnnotations(), ElementType.FIELD, "Fields");
    }

    public void checkMethod(Class<?> cls, Method method) throws AnnotationCheckException {
        Annotation[] clsDeclaredAnnotations = cls.getDeclaredAnnotations();
        checkType(cls, clsDeclaredAnnotations, method.getDeclaredAnnotations(), ElementType.METHOD, "Methods");

        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        for(int i=0;i<method.getParameterCount();i++){
            checkType(cls, clsDeclaredAnnotations, paramAnnotations[i], ElementType.PARAMETER, "Parameters");
        }
    }

    private void checkType(Class<?> cls, Annotation[] clsDeclaredAnnotations, Annotation[] declaredAnnotations, ElementType type, String name) throws AnnotationCheckException {
        for(Annotation toCheckAnnotation : declaredAnnotations){
            Class<? extends Annotation> annotationCls = toCheckAnnotation.annotationType();

            AnnotationCheckConfig config = annotationConfigMap.get(annotationCls);
            if(config == null) continue;

            if(config.whitelistClassAnnotationsMap != null){
                if(whitelistTestForAnnotations(null, config.whitelistClassAnnotationsMap, clsDeclaredAnnotations)){
                    throw new AnnotationCheckException(name + " annotated with [" + annotationCls.getName() + "] MUST be inside a class annotated with " + buildOrAndString(config.whitelistClassAnnotationsMap, null));
                }

                if(whitelistTestForAnnotations(type, config.whitelistClassAnnotationsMap, clsDeclaredAnnotations)){
                    throw new AnnotationCheckException(name + " annotated with [" + annotationCls.getName() + "] MUST be inside a class annotated with " + buildOrAndString(config.whitelistClassAnnotationsMap, type));
                }
            }

            if(config.blacklistClassAnnotationsMap != null){
                if(blacklistTestForAnnotations(null, config.blacklistClassAnnotationsMap, clsDeclaredAnnotations)){
                    throw new AnnotationCheckException(name + " annotated with [" + annotationCls.getName() + "] MUST NOT be inside a class annotated with " + buildOrAndString(config.blacklistClassAnnotationsMap, null));
                }

                if(blacklistTestForAnnotations(type, config.blacklistClassAnnotationsMap, clsDeclaredAnnotations)){
                    throw new AnnotationCheckException(name + " annotated with [" + annotationCls.getName() + "] MUST NOT be inside a class annotated with " + buildOrAndString(config.blacklistClassAnnotationsMap, type));
                }
            }

            if(config.whitelistPartnerAnnotationsMap != null){
                if(whitelistTestForAnnotations(null, config.whitelistPartnerAnnotationsMap, declaredAnnotations)){
                    throw new AnnotationCheckException(name + " annotated with [" + annotationCls.getName() + "] MUST also be annotated with " + buildOrAndString(config.whitelistPartnerAnnotationsMap, null));
                }

                if(whitelistTestForAnnotations(type, config.whitelistPartnerAnnotationsMap, declaredAnnotations)){
                    throw new AnnotationCheckException(name + " annotated with [" + annotationCls.getName() + "] MUST also be annotated with " + buildOrAndString(config.whitelistPartnerAnnotationsMap, type));
                }
            }

            if(config.blacklistPartnerAnnotationsMap != null){
                if(blacklistTestForAnnotations(null, config.blacklistPartnerAnnotationsMap, declaredAnnotations)){
                    throw new AnnotationCheckException(name + " annotated with [" + annotationCls.getName() + "] MUST NOT be annotated with " + buildOrAndString(config.blacklistPartnerAnnotationsMap, null));
                }

                if(blacklistTestForAnnotations(type, config.blacklistPartnerAnnotationsMap, declaredAnnotations)){
                    throw new AnnotationCheckException(name + " annotated with [" + annotationCls.getName() + "] MUST NOT be annotated with " + buildOrAndString(config.blacklistPartnerAnnotationsMap, type));
                }
            }

            if(config.whitelistContainingClassesMap != null){
                if(whitelistTestForContainingClass(null, config.whitelistContainingClassesMap, cls)){
                    List<Class<?>> classes = config.whitelistContainingClassesMap.get(null);
                    throw new AnnotationCheckException("The [" + annotationCls.getName() + "] annotation MUST be inside one of the following classes: [" + concatList(classes, "OR") + "]");
                }

                if(whitelistTestForContainingClass(type, config.whitelistContainingClassesMap, cls)){
                    List<Class<?>> classes = config.whitelistContainingClassesMap.get(type);
                    throw new AnnotationCheckException("The [" + annotationCls.getName() + "] annotation MUST be inside one of the following classes: [" + concatList(classes, "OR") + "]");
                }
            }

            if(config.blacklistContainingClassesMap != null){
                if(blacklistTestForContainingClass(null, config.blacklistContainingClassesMap, cls)){
                    List<Class<?>> classes = config.blacklistContainingClassesMap.get(null);
                    throw new AnnotationCheckException("The [" + annotationCls.getName() + "] annotation MUST NOT be inside any of the following classes: [" + concatList(classes, "OR") + "]");
                }

                if(blacklistTestForContainingClass(type, config.blacklistContainingClassesMap, cls)){
                    List<Class<?>> classes = config.blacklistContainingClassesMap.get(type);
                    throw new AnnotationCheckException("The [" + annotationCls.getName() + "] annotation MUST NOT be inside any of the following classes: [" + concatList(classes, "OR") + "]");
                }
            }
        }
    }

    private boolean whitelistTestForAnnotations(ElementType type, Map<ElementType, ConditionConfig> conditionMap, Annotation[] declaredAnnotations){
        ConditionConfig condition = conditionMap.get(type);
        if(condition == null) return false;
        if(condition.orAndList == null || condition.orAndList.isEmpty()) return true;

        return !testForCondition(condition.orAndList, declaredAnnotations);
    }

    private boolean blacklistTestForAnnotations(ElementType type, Map<ElementType, ConditionConfig> conditionMap, Annotation[] declaredAnnotations){
        ConditionConfig condition = conditionMap.get(type);
        if(condition == null) return false;
        if(condition.orAndList == null || condition.orAndList.isEmpty()) return false;

        return testForCondition(condition.orAndList, declaredAnnotations);
    }

    private boolean testForCondition(List<List<Class<?>>> orAndList, Annotation[] annotations){
        outerLoop:
        for(List<Class<?>> orPart : orAndList){
            for(Class<?> cls : orPart){
                if(!containsAnnotationCls(annotations, cls)) continue outerLoop;
            }

            return true;
        }

        return false;
    }

    private boolean whitelistTestForContainingClass(ElementType type, Map<ElementType, List<Class<?>>> map, Class<?> container){
        List<Class<?>> list = map.get(type);
        if(list == null) return false;

        for(Class<?> c : list){
            if(c == container){
                return false;
            }
        }

        return true;
    }

    private boolean blacklistTestForContainingClass(ElementType type, Map<ElementType, List<Class<?>>> map, Class<?> container){
        List<Class<?>> list = map.get(type);
        if(list == null) return false;

        for(Class<?> c : list){
            if(c == container){
                return true;
            }
        }

        return false;
    }





//    public void checkAllDeclared(Class<?> cls) throws AnnotationCheckException {
//        checkClass(cls);
//
//        Constructor<?>[] declaredConstructors = cls.getDeclaredConstructors();
//        for(Constructor<?> constructor : declaredConstructors){
//            checkConstructor(constructor);
//        }
//
//        Field[] declaredFields = cls.getDeclaredFields();
//        for(Field field : declaredFields){
//            checkField(field);
//        }
//
//        Method[] declaredMethods = cls.getDeclaredMethods();
//        for(Method method : declaredMethods){
//            checkMethod(method);
//        }
//    }

//    public void checkClass(Class<?> cls) throws AnnotationCheckException {
//        try {
//            Annotation[] declaredAnnotations = cls.getDeclaredAnnotations();
//
//            for (Annotation annotation : declaredAnnotations) {
//                Class<? extends Annotation> annotationCls = annotation.annotationType();
//
//                AnnotationCheckConfig config = annotationConfigMap.get(annotationCls);
//                if(config == null) continue;
//
//                if(config.whitelistClassAnnotationsMap != null) {
//                    List<Class<? extends Annotation>> allClassAnnotations = config.whitelistClassAnnotationsMap.get(null);
//                    if(allClassAnnotations != null){
//                        for(Class<? extends Annotation> classAnnotation : allClassAnnotations){
//                            if(!containsAnnotationCls(declaredAnnotations, classAnnotation)){
//                                throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST also be annotated with " + concatMsg(allClassAnnotations, "AND") + "!");
//                            }
//                        }
//                    }
//
//                    List<Class<? extends Annotation>> classAnnotations = config.whitelistClassAnnotationsMap.get(ElementType.TYPE);
//                    if(classAnnotations != null){
//                        for(Class<? extends Annotation> classAnnotation : classAnnotations){
//                            if(!containsAnnotationCls(declaredAnnotations, classAnnotation)){
//                                throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST also be annotated with " + concatMsg(classAnnotations, "AND") + "!");
//                            }
//                        }
//                    }
//                }
//
//                if(config.blacklistClassAnnotationsMap != null) {
//                    List<Class<? extends Annotation>> allClassAnnotations = config.blacklistClassAnnotationsMap.get(null);
//                    if(allClassAnnotations != null){
//                        for(Class<? extends Annotation> classAnnotation : allClassAnnotations){
//                            if(containsAnnotationCls(declaredAnnotations, classAnnotation)){
//                                throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST NOT be annotated with " + concatMsg(allClassAnnotations, "OR") + "!");
//                            }
//                        }
//                    }
//
//                    List<Class<? extends Annotation>> classAnnotations = config.blacklistClassAnnotationsMap.get(ElementType.TYPE);
//                    if(classAnnotations != null){
//                        for(Class<? extends Annotation> classAnnotation : classAnnotations){
//                            if(containsAnnotationCls(declaredAnnotations, classAnnotation)){
//                                throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST NOT be annotated with " + concatMsg(classAnnotations, "OR") + "!");
//                            }
//                        }
//                    }
//                }
//
//                if(config.whitelistPartnerAnnotationsMap != null) {
//                    List<Class<? extends Annotation>> allClassAnnotations = config.whitelistPartnerAnnotationsMap.get(null);
//                    if(allClassAnnotations != null){
//                        for(Class<? extends Annotation> classAnnotation : allClassAnnotations){
//                            if(!containsAnnotationCls(declaredAnnotations, classAnnotation)){
//                                throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST also be annotated with " + concatMsg(allClassAnnotations, "AND") + "!");
//                            }
//                        }
//                    }
//
//                    List<Class<? extends Annotation>> classAnnotations = config.whitelistPartnerAnnotationsMap.get(ElementType.TYPE);
//                    if(classAnnotations != null){
//                        for(Class<? extends Annotation> classAnnotation : classAnnotations){
//                            if(!containsAnnotationCls(declaredAnnotations, classAnnotation)){
//                                throw new AnnotationCheckException("Classes annotated with [" + annotationCls.getName() + "] MUST also be annotated with " + concatMsg(classAnnotations, "AND") + "!");
//                            }
//                        }
//                    }
//                }
//
//                throw new AnnotationCheckException("");
//
////                Check singleCheck = annotationCls.getAnnotation(Check.class);
////                if (singleCheck != null) {
////                    checkClass(cls, annotationCls, declaredAnnotations, singleCheck);
////                }
////
////                Checks multiCheck = annotationCls.getAnnotation(Checks.class);
////                if (multiCheck != null) {
////                    for(Check check : multiCheck.value()) {
////                        checkClass(cls, annotationCls, declaredAnnotations, check);
////                    }
////                }
//            }
//        }
//        catch (AnnotationCheckException e){
//            throw new AnnotationCheckException("Check failed for class: [" + cls.getName() + "]!", e);
//        }
//    }

//    private boolean helpAnnotationsContains(Annotation[] declaredAnnotations, List<Class<? extends Annotation>> annotations, boolean defaultValue) throws AnnotationCheckException {
//        if(annotations == null) return defaultValue;
//
//        for(Class<? extends Annotation> annotationCls : annotations){
//            if(containsAnnotationCls(declaredAnnotations, annotationCls)){
//                return true;
//            }
//        }
//
//        return defaultValue;
//    }

//    public void checkConstructor(Constructor<?> constructor) throws AnnotationCheckException {
//        try {
//            Class<?> declaringCls = constructor.getDeclaringClass();
//            Annotation[] clsDeclaredAnnotations = declaringCls.getDeclaredAnnotations();
//            Annotation[] declaredAnnotations = constructor.getDeclaredAnnotations();
//
//            for (Annotation annotation : declaredAnnotations) {
//                Class<? extends Annotation> annotationCls = annotation.annotationType();
//
//                Check singleCheck = annotationCls.getAnnotation(Check.class);
//                if (singleCheck != null) {
//                    checkType(declaringCls, annotationCls, clsDeclaredAnnotations, declaredAnnotations, singleCheck, ElementType.CONSTRUCTOR, "Constructors");
//                }
//
//                Checks multiCheck = annotationCls.getAnnotation(Checks.class);
//                if (multiCheck != null) {
//                    for(Check check : multiCheck.value()) {
//                        checkType(declaringCls, annotationCls, clsDeclaredAnnotations, declaredAnnotations, check, ElementType.CONSTRUCTOR, "Constructors");
//                    }
//                }
//            }
//        }
//        catch (AnnotationCheckException e){
//            throw new AnnotationCheckException("Check failed for constructor: [" + constructor + "]!", e);
//        }
//    }
//
//    public void checkField(Field field) throws AnnotationCheckException {
//        try {
//            Class<?> declaringCls = field.getDeclaringClass();
//            Annotation[] clsDeclaredAnnotations = declaringCls.getDeclaredAnnotations();
//            Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
//
//            for (Annotation annotation : declaredAnnotations) {
//                Class<? extends Annotation> annotationCls = annotation.annotationType();
//
//                Check singleCheck = annotationCls.getAnnotation(Check.class);
//                if (singleCheck != null) {
//                    checkType(declaringCls, annotationCls, clsDeclaredAnnotations, declaredAnnotations, singleCheck, ElementType.FIELD, "Fields");
//                }
//
//                Checks multiCheck = annotationCls.getAnnotation(Checks.class);
//                if (multiCheck != null) {
//                    for(Check check : multiCheck.value()) {
//                        checkType(declaringCls, annotationCls, clsDeclaredAnnotations, declaredAnnotations, check, ElementType.FIELD, "Fields");
//                    }
//                }
//            }
//        }
//        catch (AnnotationCheckException e){
//            throw new AnnotationCheckException("Check failed for field: [" + field + "]!", e);
//        }
//    }
//
//    public void checkMethod(Method method) throws AnnotationCheckException {
//        try {
//            Class<?> declaringCls = method.getDeclaringClass();
//            Annotation[] clsDeclaredAnnotations = declaringCls.getDeclaredAnnotations();
//            Annotation[] declaredAnnotations = method.getDeclaredAnnotations();
//
//            for (Annotation annotation : declaredAnnotations) {
//                Class<? extends Annotation> annotationCls = annotation.annotationType();
//
//                Check singleCheck = annotationCls.getAnnotation(Check.class);
//                if (singleCheck != null) {
//                    checkType(declaringCls, annotationCls, clsDeclaredAnnotations, declaredAnnotations, singleCheck, ElementType.METHOD, "Methods");
//                }
//
//                Checks multiCheck = annotationCls.getAnnotation(Checks.class);
//                if (multiCheck != null) {
//                    for(Check check : multiCheck.value()) {
//                        checkType(declaringCls, annotationCls, clsDeclaredAnnotations, declaredAnnotations, check, ElementType.METHOD, "Methods");
//                    }
//                }
//            }
//        }
//        catch (AnnotationCheckException e){
//            throw new AnnotationCheckException("Check failed for method: [" + method + "]!", e);
//        }
//    }
//
//
//
//
//
//
//
//
//
//
//    private void checkClass(Class<?> cls, Class<? extends Annotation> selectedAnnotation, Annotation[] declaredAnnotations, Check check) throws AnnotationCheckException {
//        if(invalidTargetType(check, ElementType.TYPE)) return;
//
//        if(!checkOrAnnotated(check.clsAnnotatedWith(), declaredAnnotations, true)){
//            throw new AnnotationCheckException("Classes annotated with [" + selectedAnnotation.getName() + "] MUST also be annotated with " + orString(check.clsAnnotatedWith()) + "!");
//        }
//
//        if(!checkOrAnnotated(check.annotatedWith(), declaredAnnotations, true)){
//            throw new AnnotationCheckException("Classes annotated with [" + selectedAnnotation.getName() + "] MUST also be annotated with " + orString(check.annotatedWith()) + "!");
//        }
//
//        if(checkOrAnnotated(check.clsNotAnnotatedWith(), declaredAnnotations, false)){
//            throw new AnnotationCheckException("Classes annotated with [" + selectedAnnotation.getName() + "] MUST NOT be annotated with " + orString(check.clsNotAnnotatedWith()) + "!");
//        }
//
//        if(checkOrAnnotated(check.notAnnotatedWith(), declaredAnnotations, false)){
//            throw new AnnotationCheckException("Classes annotated with [" + selectedAnnotation.getName() + "] MUST NOT be annotated with " + orString(check.notAnnotatedWith()) + "!");
//        }
//
//        if(!checkOrImplementing(check.clsImplementing(), cls, true)){
//            throw new AnnotationCheckException("Classes annotated with [" + selectedAnnotation.getName() + "] MUST implement " + orString(check.clsImplementing()) + "!");
//        }
//
//        if(checkOrImplementing(check.clsNotImplementing(), cls, false)){
//            throw new AnnotationCheckException("Classes annotated with [" + selectedAnnotation.getName() + "] MUST NOT implement " + orString(check.clsNotImplementing()) + "!");
//        }
//    }

//    private void checkType(Class<?> declaringCls, Class<? extends Annotation> selectedAnnotation, Annotation[] clsDeclaredAnnotations, Annotation[] declaredAnnotations, Check check, ElementType type, String name) throws AnnotationCheckException {
//        if(invalidTargetType(check, type)) return;
//
//        if(!checkOrAnnotated(check.clsAnnotatedWith(), clsDeclaredAnnotations, true)){
//            throw new AnnotationCheckException(name + " annotated with [" + selectedAnnotation.getName() + "] MUST be inside a class annotated with " + orString(check.clsAnnotatedWith()) + "!");
//        }
//
//        if(!checkOrAnnotated(check.annotatedWith(), declaredAnnotations, true)){
//            throw new AnnotationCheckException(name + " annotated with [" + selectedAnnotation.getName() + "] MUST also be annotated with " + orString(check.annotatedWith()) + "!");
//        }
//
//        if(checkOrAnnotated(check.clsNotAnnotatedWith(), clsDeclaredAnnotations, false)){
//            throw new AnnotationCheckException(name + " annotated with [" + selectedAnnotation.getName() + "] MUST NOT be inside a class annotated with " + orString(check.clsNotAnnotatedWith()) + "!");
//        }
//
//        if(checkOrAnnotated(check.notAnnotatedWith(), declaredAnnotations, false)){
//            throw new AnnotationCheckException(name + " annotated with [" + selectedAnnotation.getName() + "] MUST NOT be annotated with " + orString(check.notAnnotatedWith()) + "!");
//        }
//
//        if(!checkOrImplementing(check.clsImplementing(), declaringCls, true)){
//            throw new AnnotationCheckException(name + " annotated with [" + selectedAnnotation.getName() + "] MUST be inside a class which implements " + orString(check.clsImplementing()) + "!");
//        }
//
//        if(checkOrImplementing(check.clsNotImplementing(), declaringCls, false)){
//            throw new AnnotationCheckException(name + " annotated with [" + selectedAnnotation.getName() + "] MUST NOT be inside a class which implements " + orString(check.clsNotImplementing()) + "!");
//        }
//    }

//    private boolean checkOrAnnotated(Class<? extends Annotation>[] annotations, Annotation[] declaredAnnotations, boolean defaultValue) {
//        for(Class<? extends Annotation> annotationCls : annotations){
//            if(containsAnnotationCls(declaredAnnotations, annotationCls)){
//                return true;
//            }
//        }
//
//        return defaultValue;
//    }
//
//    private boolean checkOrAnnotated2(List<Class<? extends Annotation>> annotations, Annotation[] declaredAnnotations, boolean defaultValue) {
//        for(Class<? extends Annotation> annotationCls : annotations){
//            if(containsAnnotationCls(declaredAnnotations, annotationCls)){
//                return true;
//            }
//        }
//
//        return defaultValue;
//    }

//    private boolean checkOrImplementing(Class<?>[] toImplement, Class<?> target, boolean defaultValue){
//        for(Class<?> cls : toImplement){
//            if(isImplementing(target, cls)){
//                return true;
//            }
//        }
//
//        return defaultValue;
//    }















    private boolean containsAnnotationCls(Annotation[] annotations, Class<?> mustContain){
        for(Annotation annotation : annotations){
            if(annotation.annotationType() == mustContain) return true;
        }
        return false;
    }

//    private boolean isImplementing(Class<?> cls, Class<?> toImplement){
//        Queue<Class<?>> queue = new ArrayDeque<>();
//        queue.add(cls);
//
//        while(!queue.isEmpty()){
//            Class<?> cur = queue.poll();
//
//            Class<?>[] interfaceClasses = cur.getInterfaces();
//            for(Class<?> i : interfaceClasses){
//                if(i == toImplement) return true;
//                queue.add(i);
//            }
//
//            Class<?> superClass = cur.getSuperclass();
//            if(superClass != null){
//                if(superClass == toImplement) return true;
//                queue.add(superClass);
//            }
//        }
//
//        return false;
//    }

//    private boolean invalidTargetType(Check check, ElementType type){
//        if(check.target().length == 0) return false;
//
//        for(ElementType t : check.target()){
//            if(type == t) return false;
//        }
//        return true;
//    }

//    private String orString(Class<?>[] classes){
//        StringBuilder sb = new StringBuilder();
//        sb.append("[");
//
//        for(Class<?> cls : classes){
//            if(sb.length() != 1) sb.append("] OR [");
//            sb.append(cls.getName());
//        }
//
//        sb.append("]");
//
//        return sb.toString();
//    }
//
//    private String orString2(List<Class<? extends Annotation>> classes){
//        StringBuilder sb = new StringBuilder();
//        sb.append("[");
//
//        for(Class<?> cls : classes){
//            if(sb.length() != 1) sb.append("] OR [");
//            sb.append(cls.getName());
//        }
//
//        sb.append("]");
//
//        return sb.toString();
//    }
//
//    private String concatMsg(List<Class<? extends Annotation>> classes, String fill){
//        StringBuilder sb = new StringBuilder();
//        sb.append("[");
//
//        for(Class<?> cls : classes){
//            if(sb.length() != 1) sb.append("] ").append(fill).append(" [");
//            sb.append(cls.getName());
//        }
//
//        sb.append("]");
//
//        return sb.toString();
//    }

    private String buildOrAndString(Map<ElementType, ConditionConfig> map, ElementType type){
        ConditionConfig config = map.get(type);
        List<List<Class<?>>> orAndList = config.orAndList;

        if(orAndList.size() == 1){
            return "[" + concatList(orAndList.get(0), "AND") + "]";
        }

        StringBuilder sb = new StringBuilder();

        for(List<Class<?>> andClasses : orAndList){
            if(sb.length() != 0) sb.append(") OR (");
            sb.append(concatList(andClasses, "AND"));
        }

        return "[(" + sb + ")]";
     }

     private String concatList(List<Class<?>> andClasses, String separator){
        StringBuilder sb = new StringBuilder();

         for(Class<?> andClass : andClasses){
             if(sb.length() != 0) sb.append(" ").append(separator).append(" ");
             sb.append(andClass.getName());
         }

         return sb.toString();
     }

}
