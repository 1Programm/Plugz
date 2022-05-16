package com.programm.plugz.inject;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * A collection of {@link IClassCriteria} and {@link INameCriteria} which will be used when using the {@link UrlClassScanner}.
 */
public class ScanCriteria {

    private static class PackageCriteria implements INameCriteria {
        private final String packageName;
        private final boolean blacklisted;

        public PackageCriteria(String packageName, boolean blacklisted) {
            this.packageName = packageName;
            this.blacklisted = blacklisted;
        }

        @Override
        public boolean test(URL url, String fullClassName) {
            int lastDot = fullClassName.lastIndexOf('.');
            if(lastDot == -1) lastDot = 0;

            String packagePart = fullClassName.substring(0, lastDot);
            if(packagePart.startsWith(packageName)){
                return !blacklisted;
            }

            return blacklisted;
        }
    }

    private static class ClassImplementsCriteria implements IClassCriteria {
        private final Class<?> toImplement;
        private final int generations;

        public ClassImplementsCriteria(Class<?> toImplement, int generations) {
            this.toImplement = toImplement;
            this.generations = generations;
        }

        @Override
        public boolean test(URL url, Class<?> cls) {
            return test(cls, generations);
        }

        private boolean test(Class<?> cls, int generations){
            if(cls == null || cls == Object.class) return false;

            Class<?>[] interfaces = cls.getInterfaces();
            for(Class<?> i : interfaces){
                if(i == toImplement){
                    return true;
                }

                if(generations != 0){
                    if(test(i, generations - 1)){
                        return true;
                    }
                }
            }

            Class<?> superCls = cls.getSuperclass();
            if(superCls == toImplement){
                return true;
            }

            if(generations != 0){
                return test(superCls, generations - 1);
            }

            return false;
        }
    }

    private static class AnnotatedClassCriteria implements IClassCriteria {
        private final Class<? extends Annotation> annotatedBy;
        private final boolean searchInInterfaces;
        private final boolean searchInSuperclass;
        private final int generations;

        public AnnotatedClassCriteria(Class<? extends Annotation> annotatedBy, boolean searchInInterfaces, boolean searchInSuperclass, int generations) {
            this.annotatedBy = annotatedBy;
            this.searchInInterfaces = searchInInterfaces;
            this.searchInSuperclass = searchInSuperclass;
            this.generations = generations;
        }

        @Override
        public boolean test(URL url, Class<?> cls) {
            return test(cls, generations);
        }

        private boolean test(Class<?> cls, int generations){
            if(cls == null || cls == Object.class) return false;

            if(cls.isAnnotationPresent(annotatedBy)) return true;
            if(generations == 0) return false;

            if(searchInInterfaces) {
                Class<?>[] interfaces = cls.getInterfaces();
                for(Class<?> i : interfaces){
                    if(test(i, generations - 1)){
                        return true;
                    }
                }
            }

            if(searchInSuperclass) {
                Class<?> superCls = cls.getSuperclass();
                return test(superCls, generations - 1);
            }

            return false;
        }
    }

    /**
     * Creates the {@link ScanCriteria} with a callback which will be called when a class passed all the underlying criteria.
     * @param name the debug name of the criteria.
     * @param onSuccessCallback the callback.
     * @return an instance of the {@link ScanCriteria} class for more method chaining.
     */
    public static ScanCriteria create(String name, Consumer<Class<?>> onSuccessCallback){
        return new ScanCriteria(name, onSuccessCallback);
    }

    /**
     * Creates the {@link ScanCriteria} which will fill the provided collection when a class passed all the underlying criteria.
     * @param name the debug name of the criteria.
     * @param collection the collection to fill.
     * @return an instance of the {@link ScanCriteria} class for more method chaining.
     */
    public static ScanCriteria createOnSuccessCollect(String name, Collection<Class<?>> collection){
        return create(name, collection::add);
    }



    final String name;
    private final Consumer<Class<?>> onSuccessCallback;
    private final List<INameCriteria> nameCriteriaList = new ArrayList<>();
    private final List<IClassCriteria> classCriteriaList = new ArrayList<>();

    private ScanCriteria(String name, Consumer<Class<?>> onSuccessCallback) {
        this.name = name;
        this.onSuccessCallback = onSuccessCallback;
    }

    boolean testName(URL url, String fullClassName){
        for(INameCriteria criteria : nameCriteriaList){
            if(!criteria.test(url, fullClassName)){
                return false;
            }
        }

        return true;
    }

    boolean testClass(URL url, Class<?> cls){
        for(IClassCriteria criteria : classCriteriaList){
            if(!criteria.test(url, cls)){
                return false;
            }
        }

        return true;
    }

    void onSuccess(Class<?> cls){
        this.onSuccessCallback.accept(cls);
    }


    /**
     * Creates a criteria which permits only classes in that specific package.
     * @param packageName the package.
     * @return this instance for method chaining.
     */
    public ScanCriteria whitelistPackage(String packageName){
        nameCriteriaList.add(new PackageCriteria(packageName, false));
        return this;
    }

    /**
     * Creates a criteria which permits no classes in a specific package.
     * @param packageName the package.
     * @return this instance for method chaining.
     */
    public ScanCriteria blacklistPackage(String packageName){
        nameCriteriaList.add(new PackageCriteria(packageName, true));
        return this;
    }

    /**
     * Creates a criteria which permits only classes that implement the given class.
     * The param generations specifies how far parents should be scanned.
     * 0 means that a class must implement the given class itself.
     * 1-n means that a class must implement the given class itself or its 1-n th parent.
     * -1 means that the all parents should be checked. (-1 -> infinity)
     * @param cls the class to implement.
     * @param generations the number of generations to check through.
     * @return this instance for method chaining.
     */
    public ScanCriteria classImplements(Class<?> cls, int generations){
        classCriteriaList.add(new ClassImplementsCriteria(cls, generations));
        return this;
    }

    /**
     * Creates a criteria which permits only classes that implement the given class.
     * Deep check.
     * @param cls the class to implement.
     * @return this instance for method chaining.
     */
    public ScanCriteria classImplements(Class<?> cls){
        return classImplements(cls, -1);
    }

    /**
     * Creates a criteria which permits only classes that implement the given class.
     * Flat check: The class must implement the given class itself to be valid.
     * @param cls the class to implement.
     * @return this instance for method chaining.
     */
    public ScanCriteria classFlatImplements(Class<?> cls) {
        return classImplements(cls, 0);
    }

    /**
     * Creates a criteria which permits only classes that are annotated with the given annotation.
     * The param generations specifies how far parents should be scanned.
     * 0 means that a class must be annotated with the given annotation itself.
     * 1-n means that a class must be annotated with the given annotation itself or its 1-n th parent.
     * -1 means that the all parents should be checked. (-1 -> infinity)
     * @param cls the annotation that should be present.
     * @param generations the number of generations to check through.
     * @return this instance for method chaining.
     */
    public ScanCriteria classAnnotatedWith(Class<? extends Annotation> cls, int generations) {
        classCriteriaList.add(new AnnotatedClassCriteria(cls, true, true, generations));
        return this;
    }

    /**
     * Creates a criteria which permits only classes that are annotated with the given annotation.
     * Flat check: The class must be annotated with the given annotation itself to be valid.
     * @param cls the annotation that should be present.
     * @return this instance for method chaining.
     */
    public ScanCriteria classAnnotatedWith(Class<? extends Annotation> cls) {
        return classAnnotatedWith(cls, 0);
    }

    /**
     * Creates a criteria which permits only classes that are annotated with the given annotation.
     * Deep check.
     * @param cls the annotation that should be present.
     * @return this instance for method chaining.
     */
    public ScanCriteria classDeepAnnotatedWith(Class<? extends Annotation> cls){
        return classAnnotatedWith(cls, -1);
    }

}
