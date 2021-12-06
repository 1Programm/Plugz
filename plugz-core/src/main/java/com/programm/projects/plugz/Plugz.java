package com.programm.projects.plugz;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.NullLogger;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Plugz {

    private final Map<Class<? extends Annotation>, List<Class<?>>> annotationClasses = new HashMap<>();
    private ILogger log = new NullLogger();

    public void scan(List<URL> scanPaths, Map<URL, String> basePackageMap, List<Class<? extends Annotation>> clsAnnotations) throws ScanException {
        if(scanPaths.isEmpty()){
            log.warn("No scan paths defined.");
            return;
        }

        URL[] scanPathArray = scanPaths.toArray(new URL[0]);
        URLClassLoader classLoader = URLClassLoader.newInstance(scanPathArray);

        for (URL url : scanPathArray) {
            String basePackage = basePackageMap.getOrDefault(url, "");
            PlugzScanner.searchInUrl(log, url, basePackage, classLoader, clsAnnotations, annotationClasses);
        }
    }

    public List<Class<?>> getAnnotatedWith(Class<? extends Annotation> cls){
        return annotationClasses.get(cls);
    }

    public void setLogger(ILogger logger){
        this.log = logger;
    }

}
