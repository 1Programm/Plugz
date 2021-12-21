package com.programm.projects.plugz.magic;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.Plugz;
import com.programm.projects.plugz.ScanException;
import com.programm.projects.plugz.magic.api.*;
import com.programm.projects.plugz.magic.resource.MagicResourceException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

@Logger("Magic-Environment")
public class MagicEnvironment {

    private final Plugz plugz = new Plugz();
    private final ProxyLogger log = new ProxyLogger();
    private final MagicInstanceManager instanceManager;
    private final ScheduleManager scheduleManager;
    private final ResourcesManager resourcesManager;

    private final List<URL> searchedUrls = new ArrayList<>();
    private final Map<URL, String> searchedBases = new HashMap<>();
    private final List<Class<? extends Annotation>> searchedAnnotations = new ArrayList<>();

    private final List<URL> toSearchUrls = new ArrayList<>();
    private final Map<URL, String> toSearchBases = new HashMap<>();
    private final List<URL> toRemoveUrls = new ArrayList<>();

    private final String basePackageOfCallingClass;

    private boolean disableCallingUrl;
    private boolean notifyClassesFromRemovedUrl = true;
    private boolean addShutdownHook = true;

    public MagicEnvironment(){
        this("");
    }

    public MagicEnvironment(String basePackage){
        this.basePackageOfCallingClass = basePackage;

        this.plugz.setLogger(log);
        this.instanceManager = new MagicInstanceManager();
        this.scheduleManager = new ScheduleManager(log);
        this.resourcesManager = new ResourcesManager(log, instanceManager);

        this.searchedAnnotations.add(Service.class);
        this.searchedAnnotations.add(Resources.class);
        this.searchedAnnotations.add(Resource.class);

        registerInstance(ISchedules.class, scheduleManager);
        registerInstance(ILogger.class, log);
    }

    public void startup(){
        log.info("Starting up environment.");

        if(addShutdownHook){
            log.debug("Adding shutdown-hook...");
            addShutdownHook();
        }

        log.debug("Scanning for annotations...");
        scan();

        log.debug("Instantiating found classes.");
        instantiate();

        log.debug("Passing scheduler methods to Schedule-Manager.");
        passScheduledMethods();
    }

    public void shutdown(){
        log.info("Shutting down environment.");

        log.debug("Shutting down Schedule-Manager...");
        scheduleManager.shutdown();

        try {
            log.debug("Calling pre shutdown methods...");
            instanceManager.callPreShutdown();
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Exception while calling pre shutdown.", e);
        }

        try {
            log.debug("Shutting down Resources-Manager...");
            resourcesManager.shutdown();
        }
        catch (MagicResourceException e){
            throw new MagicRuntimeException("Exception while shutting down Resources-Manager.", e);
        }

        log.debug("Finished shutting down.");
    }

    public Changes refresh(){
        log.info("[RE]: Refreshing the environment...");
        Changes changes = new Changes();

        //Remove urls
        if(toRemoveUrls.size() > 0) {
            log.debug("[RE]: Removing [{}] urls from the environment...", toRemoveUrls.size());
            for (URL url : toRemoveUrls) {
                if (searchedUrls.contains(url)) {
                    log.trace("[RE]: Removing url [{}]...", url);

                    try {
                        Map<Class<?>, Object> removedInstances = instanceManager.removeUrl(url, notifyClassesFromRemovedUrl);
                        if (removedInstances != null) {
                            changes.removedInstancesMap.put(url, removedInstances);
                        }
                    }
                    catch (MagicInstanceException e) {
                        throw new MagicRuntimeException("Failed to remove url [" + url + "].", e);
                    }

                    scheduleManager.removeUrl(url);
                    searchedUrls.remove(url);
                    searchedBases.remove(url);

                    Map<Class<? extends Annotation>, List<Class<?>>> removedAnnotatedClasses = plugz.removeUrl(url);
                    if (removedAnnotatedClasses != null) {
                        changes.removedAnnotationClassesMap.put(url, removedAnnotatedClasses);
                    }
                }
                else if (toSearchUrls.contains(url)) {
                    toSearchUrls.remove(url);
                    toSearchBases.remove(url);
                }
            }

            toRemoveUrls.clear();
        }

        List<URL> addedUrls = new ArrayList<>(toSearchUrls);

        //Scan urls
        log.debug("[RE]: Scanning for annotations...");
        changes.newAnnotatedClassesMap = scan();

        //Instantiate
        for(URL url : addedUrls){
            List<Class<?>> serviceClasses = plugz.getAnnotatedWithFromUrl(Service.class, url);

            if(serviceClasses != null){
                log.debug("[RE]: Instantiating [{}] @Service classes from [{}].", serviceClasses.size(), url);
                for (Class<?> cls : serviceClasses) {
                    try {
                        log.trace("[RE]: Instantiating [{}]...", cls.toString());
                        Object instance = instanceManager.instantiate(cls);
                        changes.addedInstancesMap.computeIfAbsent(url, u -> new HashMap<>()).put(cls, instance);
                    }
                    catch (MagicInstanceException e) {
                        throw new MagicRuntimeException("Could not instantiate Service class: [" + cls.getName() + "] from url: [" + url + "].", e);
                    }
                }
            }

            List<Class<?>> resourceClasses = plugz.getAnnotatedWithFromUrl(Resource.class, url);

            if(resourceClasses != null) {
                log.debug("[RE]: Instantiating [{}] @Resource classes from [{}].", resourceClasses.size(), url);
                for (Class<?> cls : resourceClasses) {
                    try {
                        log.trace("[RE]: Building resource [{}]...", cls.toString());
                        Object resourceObject = resourcesManager.buildResourceObject(cls);

                        if(resourceObject != null) {
                            registerInstance(cls, resourceObject);
                            changes.addedInstancesMap.computeIfAbsent(url, u -> new HashMap<>()).put(cls, resourceObject);
                        }
                    }
                    catch (MagicResourceException e) {
                        throw new MagicRuntimeException("Could not instantiate Resource class: [" + cls.getName() + "] from url: [" + url + "].", e);
                    }
                }
            }

            List<Class<?>> resourceMergedClasses = plugz.getAnnotatedWithFromUrl(Resources.class, url);

            if(resourceMergedClasses != null) {
                log.debug("[RE]: Instantiating [{}] @Resources or classes with multiple @Resource annotations.", resourceMergedClasses.size());
                for (Class<?> cls : resourceMergedClasses) {
                    try {
                        log.trace("[RE]: Building merged resource [{}]...", cls.toString());
                        Object resourceObject = resourcesManager.buildMergedResourceObject(cls);

                        if(resourceObject != null) {
                            registerInstance(cls, resourceObject);
                            changes.addedInstancesMap.computeIfAbsent(url, u -> new HashMap<>()).put(cls, resourceObject);
                        }
                    }
                    catch (MagicResourceException e) {
                        throw new MagicRuntimeException("Could not instantiate merged Resource class: [" + cls.getName() + "].", e);
                    }
                }
            }
        }


        //Post setup phase
        try {
            log.debug("[RE]: Checking wait map...");
            instanceManager.checkWaitMap();
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Cyclic waiting dependencies could not be resolved!", e);
        }

        try {
            log.debug("[RE]: Calling post setup methods...");
            instanceManager.callPostSetupForUrls(addedUrls);
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Exception while calling post setup methods.", e);
        }

        //Schedule new Methods
        log.debug("[RE]: Passing scheduler methods to Schedule-Manager.");
        changes.addedScheduledMethods = passScheduledMethods();

        return changes;
    }

    private void addShutdownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.debug("Caught shutdown...");
            shutdown();
        }));
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
            }
            catch (ClassNotFoundException e) {
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
            log.debug("Searching through [{}] new urls.", newUrls.size());

            try {
                newAnnotatedClassesMap = plugz.scan(newUrls, newBases, searchedAnnotations);
            }
            catch (ScanException e) {
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
            log.debug("Instantiating [{}] @Service classes.", serviceClasses.size());
            for (Class<?> cls : serviceClasses) {
                try {
                    log.trace("Instantiating [{}]...", cls.toString());
                    instanceManager.instantiate(cls);
                }
                catch (MagicInstanceException e) {
                    throw new MagicRuntimeException("Could not instantiate Service class: [" + cls.getName() + "].", e);
                }
            }
        }

        List<Class<?>> resourceClasses = plugz.getAnnotatedWith(Resource.class);

        if(resourceClasses != null) {
            log.debug("Instantiating [{}] @Resource classes.", resourceClasses.size());
            for (Class<?> cls : resourceClasses) {
                try {
                    log.trace("Building resource [{}]...", cls.toString());
                    Object resourceObject = resourcesManager.buildResourceObject(cls);

                    if(resourceObject != null) {
                        registerInstance(cls, resourceObject);
                    }
                }
                catch (MagicResourceException e) {
                    throw new MagicRuntimeException("Could not instantiate Resource class: [" + cls.getName() + "].", e);
                }
            }
        }

        List<Class<?>> resourceMergedClasses = plugz.getAnnotatedWith(Resources.class);

        if(resourceMergedClasses != null) {
            log.debug("Instantiating [{}] @Resources or classes with multiple @Resource annotations.", resourceMergedClasses.size());
            for (Class<?> cls : resourceMergedClasses) {
                try {
                    log.trace("Building merged resource [{}]...", cls.toString());
                    Object resourceObject = resourcesManager.buildMergedResourceObject(cls);

                    if(resourceObject != null) {
                        registerInstance(cls, resourceObject);
                    }
                }
                catch (MagicResourceException e) {
                    throw new MagicRuntimeException("Could not instantiate merged Resource class: [" + cls.getName() + "].", e);
                }
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
        log.info("Starting post setup phase.");

        try {
            log.debug("Checking wait map...");
            instanceManager.checkWaitMap();
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Cyclic waiting dependencies could not be resolved!", e);
        }

        try {
            log.debug("Calling post setup methods...");
            instanceManager.callPostSetup();
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Exception while calling post setup methods.", e);
        }

        log.debug("Starting Schedule-Manager...");
        scheduleManager.startup();
    }

    public <T> T instantiateClass(Class<T> cls){
        try {
            return instanceManager.instantiate(cls);
        }
        catch (MagicInstanceException e) {
            throw new MagicRuntimeException("Could not instantiate class: [" + cls.getName() + "]!", e);
        }
    }

    public MagicEnvironment disableCallingUrl(){
        log.debug("Disabling caller url.");
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
            URL fromUrl = Utils.getUrlFromClass(instance.getClass());
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
            throw new MagicRuntimeException("Failed to get instance for class [" + cls + "]!", e);
        }
    }

    public IMagicMethod createMagicMethod(Object instance, Method method){
        return instanceManager.createMagicMethod(instance, method);
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

    public void disableShutdownHook(){
        this.addShutdownHook = false;
    }

}
