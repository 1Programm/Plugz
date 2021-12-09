package com.programm.projects.plugz.magic;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Changes {

    final Map<URL, Map<Class<?>, Object>> removedInstancesMap = new HashMap<>();
    final Map<URL, Map<Class<? extends Annotation>, List<Class<?>>>> removedAnnotationClassesMap = new HashMap<>();
    Map<URL, Map<Class<? extends Annotation>, List<Class<?>>>> newAnnotatedClassesMap = null;
    final Map<URL, Map<Class<?>, Object>> addedInstancesMap = new HashMap<>();
    Map<URL, List<SchedulerMethodConfig>> addedScheduledMethods = null;

    public List<Object> getRemovedInstances(){
        List<Object> instances = new ArrayList<>();

        for(URL url : removedInstancesMap.keySet()){
            Map<Class<?>, Object> instancesMap = removedInstancesMap.get(url);
            if(instancesMap == null) continue;

            for(Class<?> cls : instancesMap.keySet()){
                Object instance = instancesMap.get(cls);
                if(instance != null){
                    instances.add(instance);
                }
            }
        }

        return instances;
    }

    public List<Class<?>> getAnnotatedWith(Class<? extends Annotation> cls){
        List<Class<?>> annotatedClasses = new ArrayList<>();

        if(newAnnotatedClassesMap != null) {
            for (URL url : newAnnotatedClassesMap.keySet()) {
                Map<Class<? extends Annotation>, List<Class<?>>> classesMap = newAnnotatedClassesMap.get(url);
                if(classesMap == null) continue;

                List<Class<?>> res = classesMap.get(cls);
                if(res == null) continue;

                annotatedClasses.addAll(res);
            }
        }

        return annotatedClasses;
    }

    public Map<URL, List<Class<?>>> getAnnotatedWithMappedByUrl(Class<? extends Annotation> cls){
        Map<URL, List<Class<?>>> annotatedClasses = new HashMap<>();

        if(newAnnotatedClassesMap != null) {
            for (URL url : newAnnotatedClassesMap.keySet()) {
                Map<Class<? extends Annotation>, List<Class<?>>> classesMap = newAnnotatedClassesMap.get(url);
                if(classesMap == null) continue;

                List<Class<?>> res = classesMap.get(cls);
                if(res == null) continue;

                annotatedClasses.put(url, res);
            }
        }

        return annotatedClasses;
    }

}
