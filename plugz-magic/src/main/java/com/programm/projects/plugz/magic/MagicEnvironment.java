package com.programm.projects.plugz.magic;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.Plugz;
import com.programm.projects.plugz.ScanException;
import com.programm.projects.plugz.inject.InjectManager;
import com.programm.projects.plugz.magic.api.*;
import com.programm.projects.plugz.magic.resource.MagicResourceException;
import com.programm.projects.plugz.magic.subsystems.IInstanceManager;
import com.programm.projects.plugz.magic.subsystems.IResourcesManager;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

@Logger("Magic-Environment")
public class MagicEnvironment {

    private static final int MAX_ASYNC_WORKERS = 5;
    private static final long MAX_WORKER_SLEEP_TIME = 5000;

    private final Plugz plugz = new Plugz();
    private final ProxyLogger log = new ProxyLogger();
    private final ThreadPoolManager threadPoolManager;
    private final MagicInstanceManager instanceManager;
    private final ScheduleManager scheduleManager;
    private IResourcesManager resourcesManager;
    private final InjectManager injectionManager;

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
        this.threadPoolManager = new ThreadPoolManager(log, MAX_ASYNC_WORKERS, MAX_WORKER_SLEEP_TIME);
        this.instanceManager = new MagicInstanceManager(threadPoolManager);
        this.scheduleManager = new ScheduleManager(log, threadPoolManager);
        this.injectionManager = new InjectManager("com.programm.projects.plugz");
        this.injectionManager.setLogger(log);

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

        log.debug("Scanning for subsystems injection...");
        injectSubsystems();

        log.debug("Starting subsystems...");
        startupSubsystems();

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

        log.debug("Shutting down Thread-Pool-Manager");
        threadPoolManager.shutdown();

        try {
            log.debug("Calling pre shutdown methods...");
            instanceManager.callPreShutdown();
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Exception while calling pre shutdown.", e);
        }

        if(resourcesManager != null) {
            try {
                log.debug("Shutting down Resources-Manager...");
                resourcesManager.shutdown();
            } catch (MagicResourceException e) {
                throw new MagicRuntimeException("Exception while shutting down Resources-Manager.", e);
            }
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
                if(resourceClasses.size() != 0 && resourcesManager == null){
                    log.error("[RE]: No Resource Manager available, so no @Resource classes can be handled!");
                }
                else {
                    log.debug("[RE]: Instantiating [{}] @Resource classes from [{}].", resourceClasses.size(), url);
                    for (Class<?> cls : resourceClasses) {
                        try {
                            log.trace("[RE]: Building resource [{}]...", cls.toString());
                            Object resourceObject = resourcesManager.buildResourceObject(cls);

                            if (resourceObject != null) {
                                registerInstance(cls, resourceObject);
                                changes.addedInstancesMap.computeIfAbsent(url, u -> new HashMap<>()).put(cls, resourceObject);
                            }
                        } catch (MagicResourceException e) {
                            throw new MagicRuntimeException("Could not instantiate Resource class: [" + cls.getName() + "] from url: [" + url + "].", e);
                        }
                    }
                }
            }

            List<Class<?>> resourceMergedClasses = plugz.getAnnotatedWithFromUrl(Resources.class, url);

            if(resourceMergedClasses != null) {
                if(resourceMergedClasses.size() != 0 && resourcesManager == null){
                    log.error("[RE]: No Resource Manager available, so no @Resources classes can be handled!");
                }
                else {
                    log.debug("[RE]: Instantiating [{}] @Resources or classes with multiple @Resource annotations.", resourceMergedClasses.size());
                    for (Class<?> cls : resourceMergedClasses) {
                        try {
                            log.trace("[RE]: Building merged resource [{}]...", cls.toString());
                            Object resourceObject = resourcesManager.buildMergedResourceObject(cls);

                            if (resourceObject != null) {
                                registerInstance(cls, resourceObject);
                                changes.addedInstancesMap.computeIfAbsent(url, u -> new HashMap<>()).put(cls, resourceObject);
                            }
                        } catch (MagicResourceException e) {
                            throw new MagicRuntimeException("Could not instantiate merged Resource class: [" + cls.getName() + "].", e);
                        }
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

    private void injectSubsystems(){
        MagicInstanceManager subsystemInstanceManger = new MagicInstanceManager(threadPoolManager);

        //Needed instances for subsystems
        registerInstance(subsystemInstanceManger, ILogger.class, log);
        registerInstance(subsystemInstanceManger, IInstanceManager.class, instanceManager);

        //Find implementations
        try {
            injectionManager.scan();
        }
        catch (IOException e){
            throw new MagicRuntimeException("Injection Manager failed to scan.", e);
        }

        List<Class<? extends IResourcesManager>> resManagers = injectionManager.findImplementations(IResourcesManager.class);

        if(resManagers.size() == 1){
            Class<? extends IResourcesManager> resManagerCls = resManagers.get(0);

            log.debug("Found implementation for subsystem [resources-manager]: {}.", resManagerCls);

            try {
                this.resourcesManager = subsystemInstanceManger.instantiate(resManagerCls);
            }
            catch (MagicInstanceException e){
                throw new MagicRuntimeException("Failed to instantiate subsystem [resources-manager]:", e);
            }
        }
        else if(resManagers.size() == 0){
            log.warn("No implementation found for subsystem [resources-manager]!");
        }
        else {
            throw new MagicRuntimeException("Multiple possible implementations found for subsystem [resources-manager]:\n" + resManagers);
        }
    }

    private void startupSubsystems(){
        if(resourcesManager != null) {
            try {
                resourcesManager.startup();
            } catch (MagicResourceException e) {
                throw new MagicRuntimeException("Failed to start subsystem [resources-manager].", e);
            }
        }
    }

    private <T> T instantiateSubsystem(Class<T> cls) throws MagicInstanceException {
        try {
            Constructor<T> con = cls.getConstructor();
            return con.newInstance();
        } catch (NoSuchMethodException e) {
            throw new MagicInstanceException("No empty constructor found for class [" + cls.getName() + "]!", e);
        } catch (InvocationTargetException e) {
            throw new MagicInstanceException("Empty constructor for class [" + cls.getName() + "] threw an exception:", e);
        } catch (InstantiationException e) {
            throw new MagicInstanceException("Class [" + cls.getName() + "] could not be instantiated.", e);
        } catch (IllegalAccessException e) {
            throw new MagicInstanceException("Empty constructor for class [" + cls.getName() + "] cannot be accessed!");
        }
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
            if(resourceClasses.size() != 0 && resourcesManager == null){
                log.error("No Resource Manager available, so no @Resource classes can be handled!");
            }
            else {
                log.debug("Instantiating [{}] @Resource classes.", resourceClasses.size());
                for (Class<?> cls : resourceClasses) {
                    try {
                        log.trace("Building resource [{}]...", cls.toString());
                        Object resourceObject = resourcesManager.buildResourceObject(cls);

                        if (resourceObject != null) {
                            registerInstance(cls, resourceObject);
                        }
                    } catch (MagicResourceException e) {
                        throw new MagicRuntimeException("Could not instantiate Resource class: [" + cls.getName() + "].", e);
                    }
                }
            }
        }

        List<Class<?>> resourceMergedClasses = plugz.getAnnotatedWith(Resources.class);

        if(resourceMergedClasses != null) {
            if(resourceMergedClasses.size() != 0 && resourcesManager == null){
                log.error("No Resource Manager available, so no @Resources classes can be handled!");
            }
            else {
                log.debug("Instantiating [{}] @Resources or classes with multiple @Resource annotations.", resourceMergedClasses.size());
                for (Class<?> cls : resourceMergedClasses) {
                    try {
                        log.trace("Building merged resource [{}]...", cls.toString());
                        Object resourceObject = resourcesManager.buildMergedResourceObject(cls);

                        if (resourceObject != null) {
                            registerInstance(cls, resourceObject);
                        }
                    } catch (MagicResourceException e) {
                        throw new MagicRuntimeException("Could not instantiate merged Resource class: [" + cls.getName() + "].", e);
                    }
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
            throw new MagicRuntimeException("Waiting dependencies could not be resolved. " + e.getMessage());
        }

        log.debug("Starting Schedule-Manager...");
        scheduleManager.startup();

        try {
            log.debug("Calling post setup methods...");
            instanceManager.callPostSetup();
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Exception while calling post setup methods.", e);
        }
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
        return registerInstance(instanceManager, cls, instance);
    }

    private MagicEnvironment registerInstance(MagicInstanceManager manager, Class<?> cls, Object instance){
        try {
            URL fromUrl = Utils.getUrlFromClass(instance.getClass());
            manager.registerInstance(fromUrl, cls, instance);
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
