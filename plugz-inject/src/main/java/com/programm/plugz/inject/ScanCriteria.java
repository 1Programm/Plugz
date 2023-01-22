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
        private final URL forURL;
        private final String[] packageNames;
        private final boolean blacklisted;

        public PackageCriteria(URL forURL, String[] packageNames, boolean blacklisted) {
            this.forURL = forURL;
            this.packageNames = packageNames;
            this.blacklisted = blacklisted;
        }

        @Override
        public boolean test(URL url, String fullClassName) {
            if(forURL != null && forURL != url) return true;

            int lastDot = fullClassName.lastIndexOf('.');
            if(lastDot == -1) lastDot = 0;

            String packagePart = fullClassName.substring(0, lastDot);

            for(String pkg : packageNames){
                if(packagePart.startsWith(pkg) == blacklisted) return false;
            }

            return true;
//            if(blacklisted){
//                for(String pkg : packageNames){
//                    if(packagePart.startsWith(pkg)) return false;
//                }
//
//                return true;
//            }
//            else {
//                for(String pkg : packageNames){
//                    if(!packagePart.startsWith(pkg)) return false;
//                }
//                return true;
//            }
        }
    }

    private static class URLCriteria implements INameCriteria {
        private final URL[] urls;
        private final boolean blacklisted;

        public URLCriteria(URL[] urls, boolean blacklisted) {
            this.urls = urls;
            this.blacklisted = blacklisted;
        }

        @Override
        public boolean test(URL url, String fullClassName) {
            for(URL testUrl : urls){
                if(testUrl.sameFile(url)) return !blacklisted;
            }

            return blacklisted;
        }
    }


    private static class ClassCriteria implements IClassCriteria {
        private final URL forURL;
        private final Class<?>[] classes;
        private final boolean blacklisted;

        public ClassCriteria(URL forURL, Class<?>[] classes, boolean blacklisted) {
            this.forURL = forURL;
            this.classes = classes;
            this.blacklisted = blacklisted;
        }

        @Override
        public boolean test(URL url, Class<?> cls) {
            if(forURL != null && forURL != url) return true;

            for(Class<?> c : classes){
                if(cls == c) return !blacklisted;
            }

            return blacklisted;
        }
    }

    private static class ClassImplementsCriteria implements IClassCriteria {
        private final URL forURL;
        private final Class<?> toImplement;
        private final int generations;

        public ClassImplementsCriteria(URL forURL, Class<?> toImplement, int generations) {
            this.forURL = forURL;
            this.toImplement = toImplement;
            this.generations = generations;
        }

        @Override
        public boolean test(URL url, Class<?> cls) {
            if(forURL != null && forURL != url) return true;
            return ClassScanUtils.implementsClass(cls, toImplement, generations);
        }
    }

    private static class AnnotatedClassCriteria implements IClassCriteria {
        private final URL forURL;
        private final Class<? extends Annotation> annotatedBy;
        private final boolean searchInInterfaces;
        private final boolean searchInSuperclass;
        private final int generations;
        private final boolean blacklisted;

        public AnnotatedClassCriteria(URL forURL, Class<? extends Annotation> annotatedBy, boolean searchInInterfaces, boolean searchInSuperclass, int generations, boolean blacklisted) {
            this.forURL = forURL;
            this.annotatedBy = annotatedBy;
            this.searchInInterfaces = searchInInterfaces;
            this.searchInSuperclass = searchInSuperclass;
            this.generations = generations;
            this.blacklisted = blacklisted;
        }

        @Override
        public boolean test(URL url, Class<?> cls) {
            if(forURL != null && forURL != url) return true;
            return ClassScanUtils.annotatedWith(cls, annotatedBy, searchInInterfaces, searchInSuperclass, generations) != blacklisted;
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
     * Creates a criteria which permits only the specific urls.
     * @param urls the urls.
     * @return this instance for method chaining.
     */
    public ScanCriteria whitelistURLs(URL... urls){
        nameCriteriaList.add(new URLCriteria(urls, false));
        return this;
    }

    /**
     * Creates a criteria which permits no urls that are specified.
     * @param urls the urls.
     * @return this instance for method chaining.
     */
    public ScanCriteria blacklistURLs(URL... urls){
        nameCriteriaList.add(new URLCriteria(urls, true));
        return this;
    }

    /**
     * Creates a criteria which permits only classes in the specific packages.
     * @param packageNames the packages.
     * @return this instance for method chaining.
     */
    public ScanCriteria whitelistPackages(String... packageNames){
        nameCriteriaList.add(new PackageCriteria(null, packageNames, false));
        return this;
    }

    /**
     * Creates a criteria which permits no classes in the specific packages.
     * @param packageNames the packages.
     * @return this instance for method chaining.
     */
    public ScanCriteria blacklistPackages(String... packageNames){
        nameCriteriaList.add(new PackageCriteria(null, packageNames, true));
        return this;
    }

    /**
     * Creates a criteria which only permits specific classes for some URL.
     * @param url the url in which the classes are found.
     * @param classes the class to be blacklisted.
     * @return this instance for method chaining.
     */
    public ScanCriteria whitelistClassesForURL(URL url, Class<?>... classes){
        classCriteriaList.add(new ClassCriteria(url, classes, false));
        return this;
    }

    /**
     * Creates a criteria which blacklists specific classes for some URL.
     * @param url the url in which the classes are found.
     * @param classes the class to be blacklisted.
     * @return this instance for method chaining.
     */
    public ScanCriteria blacklistClassesForURL(URL url, Class<?>... classes){
        classCriteriaList.add(new ClassCriteria(url, classes, true));
        return this;
    }

    /**
     * Creates a criteria which blacklists specific classes.
     * @param classes the class to be blacklisted.
     * @return this instance for method chaining.
     */
    public ScanCriteria blacklistClasses(Class<?>... classes){
        classCriteriaList.add(new ClassCriteria(null, classes, true));
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
        classCriteriaList.add(new ClassImplementsCriteria(null, cls, generations));
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
     * Creates a criteria which permits only classes that are NOT annotated with the given annotation.
     * The param generations specifies how far parents should be scanned.
     * 0 means that a class must be annotated with the given annotation itself.
     * 1-n means that a class must be annotated with the given annotation itself or its 1-n th parent.
     * -1 means that the all parents should be checked. (-1 -> infinity)
     * @param cls the annotation that should be present.
     * @param generations the number of generations to check through.
     * @return this instance for method chaining.
     */
    public ScanCriteria classNotAnnotatedWith(Class<? extends Annotation> cls, int generations) {
        classCriteriaList.add(new AnnotatedClassCriteria(null, cls, true, true, generations, true));
        return this;
    }

    /**
     * Creates a criteria which permits only classes that are NOT annotated with the given annotation.
     * Flat check: The class must be annotated with the given annotation itself to be valid.
     * @param cls the annotation that should be present.
     * @return this instance for method chaining.
     */
    public ScanCriteria classNotAnnotatedWith(Class<? extends Annotation> cls) {
        return classNotAnnotatedWith(cls, 0);
    }

    /**
     * Creates a criteria which permits only classes that are NOT annotated with the given annotation.
     * Deep check.
     * @param cls the annotation that should be present.
     * @return this instance for method chaining.
     */
    public ScanCriteria classNotDeepAnnotatedWith(Class<? extends Annotation> cls){
        return classNotAnnotatedWith(cls, -1);
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
        classCriteriaList.add(new AnnotatedClassCriteria(null, cls, true, true, generations, false));
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
