package com.programm.projects.plugz;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.NullLogger;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Plugz {

    public final Map<URL, PlugzScanner> urlScannerMap = new HashMap<>();
    private ILogger log = new NullLogger();

    public Map<URL, Map<Class<? extends Annotation>, List<Class<?>>>> scan(List<URL> scanPaths, Map<URL, String> basePackageMap, List<Class<? extends Annotation>> clsAnnotations) throws ScanException {
        if(scanPaths.isEmpty()){
            log.warn("No scan paths defined.");
            return null;
        }

        Map<URL, Map<Class<? extends Annotation>, List<Class<?>>>> newAnnotatedClassesMap = new HashMap<>();

        URL[] scanPathArray = scanPaths.toArray(new URL[0]);
        URLClassLoader classLoader = URLClassLoader.newInstance(scanPathArray);

        for (URL url : scanPathArray) {
            PlugzScanner scanner = urlScannerMap.get(url);

            if(scanner == null) {
                scanner = new PlugzScanner();
                urlScannerMap.put(url, scanner);
            }

            String basePackage = basePackageMap.getOrDefault(url, "");
            Map<Class<? extends Annotation>, List<Class<?>>> newAnnotatedClasses = scanner.searchInUrl(log, url, basePackage, classLoader, clsAnnotations);

            newAnnotatedClassesMap.put(url, newAnnotatedClasses);
        }

        return newAnnotatedClassesMap;
    }

    public Map<Class<? extends Annotation>, List<Class<?>>> removeUrl(URL url){
        PlugzScanner scanner = urlScannerMap.remove(url);

        if(scanner == null) return null;

        return scanner.foundAnnotationClasses;
    }

    public List<Class<?>> getAnnotatedWith(Class<? extends Annotation> cls){
        List<Class<?>> foundClasses = new ArrayList<>();

        for(URL url : urlScannerMap.keySet()){
            List<Class<?>> classes = urlScannerMap.get(url).foundAnnotationClasses.get(cls);
            if(classes != null) {
                foundClasses.addAll(classes);
            }
        }

        return foundClasses;
    }

    public List<Class<?>> getAnnotatedWithFromUrl(Class<? extends Annotation> cls, URL url){
        PlugzScanner scanner = urlScannerMap.get(url);

        if(scanner == null) return null;

        return scanner.foundAnnotationClasses.get(cls);
    }

    public void setLogger(ILogger logger){
        this.log = logger;
    }

}
