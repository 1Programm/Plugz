package com.programm.plugz.annocheck;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationChecker {

    private class ForAnnotationClassConfig implements IForAnnotationClassConfig {
        private final AnnotationCheckConfig config;
        private final ElementType[] types;

        ForAnnotationClassConfig(Class<? extends Annotation> annotationClass, ElementType[] types) {
            this.config = annotationConfigMap.computeIfAbsent(annotationClass, t -> new AnnotationCheckConfig());
            this.types = types;
        }

        @Override
        public IClassAnnotations classAnnotations() {
            return new ClassAnnotations(config, types);
        }

        @Override
        public IPartnerAnnotations partnerAnnotations(){
            return new PartnerAnnotations(config, types);
        }

        @Override
        public IContainingClasses containingClasses(){
            return new ContainingClasses(config, types);
        }

    }

    private class ClassAnnotations implements IClassAnnotations {
        private final AnnotationCheckConfig config;
        private final ElementType[] types;

        public ClassAnnotations(AnnotationCheckConfig config, ElementType[] types) {
            this.config = config;
            this.types = types;
        }

        @Override
        public ICondition whitelist() {
            if(config.whitelistClassAnnotationsMap == null) config.whitelistClassAnnotationsMap = new HashMap<>();
            return helpConfig(config.whitelistClassAnnotationsMap, types);
        }

        @Override
        public ICondition blacklist() {
            if(config.blacklistClassAnnotationsMap == null) config.blacklistClassAnnotationsMap = new HashMap<>();
            return helpConfig(config.blacklistClassAnnotationsMap, types);
        }

    }

    private class PartnerAnnotations implements IPartnerAnnotations{
        private final AnnotationCheckConfig config;
        private final ElementType[] types;

        public PartnerAnnotations(AnnotationCheckConfig config, ElementType[] types) {
            this.config = config;
            this.types = types;
        }

        @Override
        public ICondition whitelist() {
            if(config.whitelistPartnerAnnotationsMap == null) config.whitelistPartnerAnnotationsMap = new HashMap<>();
            return helpConfig(config.whitelistPartnerAnnotationsMap, types);
        }

        @Override
        public ICondition blacklist() {
            if(config.blacklistPartnerAnnotationsMap == null) config.blacklistPartnerAnnotationsMap = new HashMap<>();
            return helpConfig(config.blacklistPartnerAnnotationsMap, types);
        }
    }

    private static class ContainingClasses implements IContainingClasses {
        private final AnnotationCheckConfig config;
        private final ElementType[] types;

        public ContainingClasses(AnnotationCheckConfig config, ElementType[] types) {
            this.config = config;
            this.types = types;
        }

        @Override
        public void whitelist(Class<?>... classes) {
            if(config.whitelistContainingClassesMap == null) config.whitelistContainingClassesMap = new HashMap<>();
            for(Class<?> cls : classes) {
                if ((types == null || types.length == 0)) {
                    config.whitelistContainingClassesMap.computeIfAbsent(null, t -> new ArrayList<>()).add(cls);
                } else {
                    for (ElementType type : types) {
                        config.whitelistContainingClassesMap.computeIfAbsent(type, t -> new ArrayList<>()).add(cls);
                    }
                }
            }
        }

        @Override
        public void blacklist(Class<?>... classes) {
            if(config.blacklistContainingClassesMap == null) config.blacklistContainingClassesMap = new HashMap<>();
            for(Class<?> cls : classes) {
                if((types == null || types.length == 0)) {
                    config.blacklistContainingClassesMap.computeIfAbsent(null, t -> new ArrayList<>()).add(cls);
                }
                else {
                    for(ElementType type : types) {
                        config.blacklistContainingClassesMap.computeIfAbsent(type, t -> new ArrayList<>()).add(cls);
                    }
                }
            }
        }
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
        private final List<List<Class<?>>> orAndList = new ArrayList<>();
        private boolean sealed;

        @Override
        public ConditionConfig set(Class<?> cls){
            if(sealed) throw new IllegalStateException("This configuration is sealed!");

            orAndList.clear();

            List<Class<?>> classes = new ArrayList<>();
            classes.add(cls);
            orAndList.add(classes);

            return this;
        }

        @Override
        public ConditionConfig and(Class<?> cls){
            if(sealed) throw new IllegalStateException("This configuration is sealed!");

            if(orAndList.isEmpty()) return set(cls);
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



    public IForAnnotationClassConfig forClass(Class<? extends Annotation> target, ElementType... types){
        return new ForAnnotationClassConfig(target, types);
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

        return !testForCondition(condition.orAndList, declaredAnnotations);
    }

    private boolean blacklistTestForAnnotations(ElementType type, Map<ElementType, ConditionConfig> conditionMap, Annotation[] declaredAnnotations){
        ConditionConfig condition = conditionMap.get(type);
        if(condition == null) return false;

        return testForCondition(condition.orAndList, declaredAnnotations);
    }

    private boolean testForCondition(List<List<Class<?>>> orAndList, Annotation[] annotations){
        if(orAndList.isEmpty()) return annotations.length == 1;

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

















    private boolean containsAnnotationCls(Annotation[] annotations, Class<?> mustContain){
        for(Annotation annotation : annotations){
            if(annotation.annotationType() == mustContain) return true;
        }
        return false;
    }

    private String buildOrAndString(Map<ElementType, ConditionConfig> map, ElementType type){
        ConditionConfig config = map.get(type);
        List<List<Class<?>>> orAndList = config.orAndList;

        if(orAndList.isEmpty()) return "[No other annotation]";

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
