package com.programm.projects.plugz.magic;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.plugz.Plugz;
import com.programm.projects.plugz.ScanException;
import com.programm.projects.plugz.magic.api.IMagicMethod;
import com.programm.projects.plugz.magic.api.ISchedules;
import com.programm.projects.plugz.magic.api.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class MagicEnvironment {

    private final Plugz plugz = new Plugz();
    private final ProxyLogger log = new ProxyLogger();
    private final MagicInstanceManager instanceManager;
    private final ScheduleManager scheduleManager;

    private final List<URL> searchedUrls = new ArrayList<>();
    private final Map<URL, String> searchedBases = new HashMap<>();
    private final List<Class<? extends Annotation>> searchedAnnotations = new ArrayList<>();

    private final List<URL> toSearchUrls = new ArrayList<>();
    private final Map<URL, String> toSearchBases = new HashMap<>();
    private final List<URL> toRemoveUrls = new ArrayList<>();

    private final String basePackageOfCallingClass;
    private boolean disableCallingUrl;

    private boolean notifyClassesFromRemovedUrl = true;

    public MagicEnvironment(){
        this("");
    }

    public MagicEnvironment(String basePackage){
        this.plugz.setLogger(log);
        this.instanceManager = new MagicInstanceManager();
        this.scheduleManager = new ScheduleManager();
        this.basePackageOfCallingClass = basePackage;

        this.searchedAnnotations.add(Service.class);

        registerInstance(ISchedules.class, scheduleManager);
        registerInstance(ILogger.class, log);
    }

    public MagicEnvironment disableCallingUrl(){
        disableCallingUrl = true;
        return this;
    }

    public void addUrl(URL url, String basePackage){
        toSearchUrls.add(url);
        toSearchBases.put(url, basePackage);
    }

    public void removeUrl(URL url){
        toRemoveUrls.add(url);
    }

    public MagicEnvironment addSearchAnnotation(Class<? extends Annotation> cls){
        searchedAnnotations.add(cls);
        return this;
    }

    public MagicEnvironment registerInstance(Class<?> cls, Object instance){
        try {
            URL fromUrl = MagicInstanceManager.getUrlFromClass(instance.getClass());
            instanceManager.registerInstance(fromUrl, cls, instance);
            return this;
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Could not register instance of class: [" + cls.getName() + "]!", e);
        }
    }

    public <T> T getInstance(Class<T> cls){
        try {
            return instanceManager.getInstance(cls);
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Failed to get instance!", e);
        }
    }

    public IMagicMethod createMagicMethod(Object instance, Method method){
        return instanceManager.createMagicMethod(instance, method);
    }

    public void startup(){
        scan();
        instantiate();
        passScheduledMethods();
    }

    public void shutdown(){
        scheduleManager.shutdown();

        try {
            instanceManager.callPreShutdown();
        } catch (MagicInstanceException e){
            throw new MagicRuntimeException("Exception while calling pre shutdown.", e);
        }
    }

    public Changes refresh(){
        Changes changes = new Changes();

        //Remove urls
        for(URL url : toRemoveUrls){
            if(searchedUrls.contains(url)){
                try {
                    Map<Class<?>, Object> removedInstances = instanceManager.removeUrl(url, notifyClassesFromRemovedUrl);
                    if(removedInstances != null){
                        changes.removedInstancesMap.put(url, removedInstances);
                    }
                }
                catch (MagicInstanceException e){
                    throw new MagicRuntimeException("Failed to remove url [" + url + "].", e);
                }

                scheduleManager.removeUrl(url);
                searchedUrls.remove(url);
                searchedBases.remove(url);

                Map<Class<? extends Annotation>, List<Class<?>>> removedAnnotatedClasses = plugz.removeUrl(url);
                if(removedAnnotatedClasses != null){
                    changes.removedAnnotationClassesMap.put(url, removedAnnotatedClasses);
                }
            }
            else if(toSearchUrls.contains(url)){
                toSearchUrls.remove(url);
                toSearchBases.remove(url);
            }
        }
        toRemoveUrls.clear();

        List<URL> addedUrls = new ArrayList<>(toSearchUrls);

        //Scan urls
        changes.newAnnotatedClassesMap = scan();

        //Instantiate
        for(URL url : addedUrls){
            List<Class<?>> classes = plugz.getAnnotatedWithFromUrl(Service.class, url);
            if(classes != null){
                try {
                    for (Class<?> cls : classes) {
                        Object instance = instanceManager.instantiate(cls);
                        changes.addedInstancesMap.computeIfAbsent(url, u -> new HashMap<>()).put(cls, instance);
                    }
                } catch (MagicInstanceException e) {
                    throw new MagicRuntimeException("Could not instantiate Service classes from url: [" + url + "].", e);
                }
            }
        }


        //Post setup phase
        try {
            instanceManager.checkWaitMap();
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Cyclic waiting dependencies could not be resolved!", e);
        }

        try {
            instanceManager.callPostSetupForUrls(addedUrls);
        } catch (MagicInstanceException e){
            throw new MagicRuntimeException("Exception while calling post setup methods.", e);
        }

        //Schedule new Methods
        changes.addedScheduledMethods = passScheduledMethods();

        return changes;
    }

    private Map<URL, Map<Class<? extends Annotation>, List<Class<?>>>> scan(){
        List<URL> urls = new ArrayList<>(this.toSearchUrls);
        Map<URL, String> bases = new HashMap<>(this.toSearchBases);
        toSearchUrls.clear();
        toSearchBases.clear();

        if(!disableCallingUrl) {
            StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
            String _callingClass = ste.getClassName();
            try {
                Class<?> callingClass = MagicEnvironment.class.getClassLoader().loadClass(_callingClass);
                URL url = callingClass.getProtectionDomain().getCodeSource().getLocation();
                urls.add(url);
                bases.put(url, basePackageOfCallingClass);
            } catch (ClassNotFoundException e) {
                throw new MagicRuntimeException("INVALID STATE: Should always calling class", e);
            }
        }

        List<URL> newUrls = new ArrayList<>();
        Map<URL, String> newBases = new HashMap<>();

        for(URL url : urls){
            if(!searchedUrls.contains(url)){
                String newBase = bases.get(url);
                newUrls.add(url);
                newBases.put(url, newBase);
            }
        }

        Map<URL, Map<Class<? extends Annotation>, List<Class<?>>>> newAnnotatedClassesMap = null;

        if(newUrls.size() > 0) {
            try {
                newAnnotatedClassesMap = plugz.scan(newUrls, newBases, searchedAnnotations);
            } catch (ScanException e) {
                throw new IllegalStateException("Failed to execute scan: " + e.getMessage(), e);
            }

            searchedUrls.addAll(newUrls);
            searchedBases.putAll(newBases);
        }

        return newAnnotatedClassesMap;
    }

    private void instantiate(){
        List<Class<?>> serviceClasses = plugz.getAnnotatedWith(Service.class);

        if(serviceClasses != null) {
            try {
                for (Class<?> cls : serviceClasses) {
                    instanceManager.instantiate(cls);
                }
            } catch (MagicInstanceException e) {
                throw new MagicRuntimeException("Could not instantiate Service classes.", e);
            }
        }
    }

    private Map<URL, List<SchedulerMethodConfig>> passScheduledMethods(){
        Map<URL, List<SchedulerMethodConfig>> configs = new HashMap<>(instanceManager.toScheduleMethods);
        instanceManager.toScheduleMethods.clear();

        for(URL url : configs.keySet()) {
            for(SchedulerMethodConfig config : configs.get(url)) {
                scheduleManager.scheduleRunnable(url, config);
            }
        }

        return configs;
    }

    public void postSetup() {
        try {
            instanceManager.checkWaitMap();
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Cyclic waiting dependencies could not be resolved!", e);
        }

        try {
            instanceManager.callPostSetup();
        } catch (MagicInstanceException e){
            throw new MagicRuntimeException("Exception while calling post setup methods.", e);
        }

        scheduleManager.startup();
    }

    public <T> T instantiateClass(Class<T> cls){
        try {
            return instanceManager.instantiate(cls);
        } catch (MagicInstanceException e) {
            throw new MagicRuntimeException("Could not instantiate class: [" + cls.getName() + "]!", e);
        }
    }

    public List<Class<?>> getAnnotatedWith(Class<? extends Annotation> cls){
        return plugz.getAnnotatedWith(cls);
    }

    public void setLogger(ILogger logger){
        log.setLogger(logger);
    }

    public void setNotifyClassesFromRemovedUrl(boolean notify){
        this.notifyClassesFromRemovedUrl = notify;
    }

}
