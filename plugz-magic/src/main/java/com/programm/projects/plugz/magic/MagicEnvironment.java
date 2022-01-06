package com.programm.projects.plugz.magic;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.core.Plugz;
import com.programm.projects.plugz.core.ScanException;
import com.programm.projects.plugz.inject.InjectManager;
import com.programm.projects.plugz.magic.api.*;
import com.programm.projects.plugz.magic.api.db.*;
import com.programm.projects.plugz.magic.api.resources.IResourcesManager;
import com.programm.projects.plugz.magic.api.resources.MagicResourceException;
import com.programm.projects.plugz.magic.api.resources.Resource;
import com.programm.projects.plugz.magic.api.resources.Resources;
import com.programm.projects.plugz.magic.api.schedules.IScheduleManager;
import com.programm.projects.plugz.magic.api.schedules.ISchedules;
import com.programm.projects.plugz.magic.api.schedules.ScheduledMethodConfig;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Logger("Magic-Environment")
public class MagicEnvironment {

    private static final int MAX_ASYNC_WORKERS = 5;
    private static final long MAX_WORKER_SLEEP_TIME = 5000;
    private static final String DEFAULT_INJECTION_POOL = "com.programm.projects.plugz";

    private final Plugz plugz = new Plugz();
    private final ProxyLogger log = new ProxyLogger();
    private final ThreadPoolManager threadPoolManager;
    private final MagicInstanceManager instanceManager;
    private final InjectManager injectionManager;
    private final Subsystems subsystems;
    private IScheduleManager scheduleManager;
    private IResourcesManager resourcesManager;
    private IDatabaseManager databaseManager;

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
    private boolean logSubsystemMissing = true;

    public MagicEnvironment(String... args){
        this("", args);
    }

    public MagicEnvironment(String basePackage, String... args){
        this.basePackageOfCallingClass = basePackage;

        int max_async_workers = MAX_ASYNC_WORKERS;
        long max_worker_sleep = MAX_WORKER_SLEEP_TIME;
        String injection_pool = DEFAULT_INJECTION_POOL;

        for(int i=0;i<args.length;i++){
            String arg = args[i];
            switch (arg) {
                case "-disableCallingUrl":
                    disableCallingUrl = true;
                    break;
                case "-noShutdownHook":
                    addShutdownHook = false;
                    break;
                case "-ignoreSubsystemMissing":
                    logSubsystemMissing = false;
                    break;
                case "-maxAsyncWorkers":
                    if(i + 1 >= args.length) {
                        System.err.println("Invalid arguments! [-maxAsyncWorkers] should be followed by a positive integer!");
                        continue;
                    }

                    try {
                        max_async_workers = Integer.parseInt(args[i + 1]);
                    }
                    catch (NumberFormatException e){
                        System.err.println("Invalid arguments! [-maxAsyncWorkers] should be followed by a positive integer!");
                        continue;
                    }
                    break;
                case "-maxWorkerSleep":
                    if(i + 1 >= args.length) {
                        System.err.println("Invalid arguments! [-maxWorkerSleep] should be followed by a positive long!");
                        continue;
                    }

                    try {
                        max_worker_sleep = Long.parseLong(args[i + 1]);
                    }
                    catch (NumberFormatException e){
                        System.err.println("Invalid arguments! [-maxWorkerSleep] should be followed by a positive integer!");
                        continue;
                    }
                    break;
                case "-injectionPool":
                    if(i + 1 >= args.length) {
                        System.err.println("Invalid arguments! [-injectionPool] should be followed by a String!");
                        continue;
                    }

                    injection_pool = args[i + 1];
                    break;
            }
        }

        this.plugz.setLogger(log);
        this.threadPoolManager = new ThreadPoolManager(log, max_async_workers, max_worker_sleep);
        this.instanceManager = new MagicInstanceManager(threadPoolManager);
        this.injectionManager = new InjectManager(injection_pool);
        this.injectionManager.setLogger(log);
        this.subsystems = new Subsystems(log);

        this.searchedAnnotations.add(Service.class);
        this.searchedAnnotations.add(Set.class);
        this.searchedAnnotations.add(Resources.class);
        this.searchedAnnotations.add(Resource.class);
        this.searchedAnnotations.add(Entity.class);
        this.searchedAnnotations.add(Repo.class);

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

        if(scheduleManager != null) log.debug("Passing scheduler methods to Schedule-Manager.");
        passScheduledMethods();
    }

    public void shutdown(){
        log.info("Shutting down environment.");

        log.debug("Shutting down Thread-Pool-Manager");
        threadPoolManager.shutdown();

        try {
            log.debug("Calling pre shutdown methods...");
            instanceManager.callPreShutdown();
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Exception while calling pre shutdown.", e);
        }

        subsystems.shutdown();

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

                    if(databaseManager != null) databaseManager.removeUrl(url);
                    if(resourcesManager != null) resourcesManager.removeUrl(url);
                    if(scheduleManager != null) scheduleManager.removeUrl(url);
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
            List<Class<?>> setClasses = plugz.getAnnotatedWithFromUrl(Set.class, url);

            if(setClasses != null){
                log.debug("[RE]: Instantiating [{}] @Set classes.", setClasses.size());
                for(Class<?> cls : setClasses){
                    try {
                        log.trace("[RE]: Register [{}] as @Set class...", cls.toString());
                        instanceManager.registerSetClass(cls);
                        //TODO: add to changes
                    }
                    catch (MagicInstanceException e) {
                        throw new MagicRuntimeException("Could not instantiate Service class: [" + cls.getName() + "] from url: [" + url + "].", e);
                    }
                }
            }

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

            List<Class<?>> entityClasses = plugz.getAnnotatedWithFromUrl(Entity.class, url);

            if(entityClasses != null){
                if(entityClasses.size() != 0 && databaseManager == null){
                    log.error("[RE]: No Database Manager available, so no @Entity classes can be handled!");
                }
                else {
                    log.debug("[RE]: Registering [{}] @Entity classes.", entityClasses.size());
                    for(Class<?> cls : entityClasses){
                        try {
                            log.trace("[RE]: Registering entity [{}]...", cls);
                            databaseManager.registerEntity(cls);
                            //TODO: add to changes
                        }
                        catch (DataBaseException e){
                            throw new MagicRuntimeException("Could not register Entity: [" + cls.getName() + "]!", e);
                        }
                    }
                }
            }

            List<Class<?>> repoClasses = plugz.getAnnotatedWithFromUrl(Repo.class, url);

            if(repoClasses != null){
                if(repoClasses.size() != 0 && databaseManager == null){
                    log.error("[RE]: No Database Manager available, so no @Repo classes can be handled!");
                }
                else {
                    log.debug("[RE]: Registering [{}] @Repo classes.", repoClasses.size());
                    for(Class<?> cls : repoClasses){
                        Object repoImplementation;
                        try {
                            log.trace("[RE]: Creating repository [{}]...", cls);
                            repoImplementation = databaseManager.registerAndImplementRepo(cls);
                        }
                        catch (DataBaseException e){
                            throw new MagicRuntimeException("Could not create repository from class: [" + cls.getName() + "].", e);
                        }

                        if(repoImplementation == null){
                            throw new MagicRuntimeException("Database Manager returned null implementation for repository class: [" + cls.getName() + "].");
                        }

                        log.trace("[RE]: Registering repository implementation [{}]...", repoImplementation);
                        registerInstance(cls, repoImplementation);
                        //TODO: add to changes
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
            throw new MagicRuntimeException("Waiting dependencies could not be resolved. " + e.getMessage());
        }

        try {
            log.debug("[RE]: Calling post setup methods...");
            instanceManager.callPostSetupForUrls(addedUrls);
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Exception while calling post setup methods.", e);
        }

        //Schedule new Methods
        if(scheduleManager != null) log.debug("[RE]: Passing scheduler methods to Schedule-Manager.");
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
        //Find implementations
        try {
            injectionManager.scan();
        }
        catch (IOException e){
            throw new MagicRuntimeException("Injection Manager failed to scan.", e);
        }

        subsystems.inject(instanceManager, threadPoolManager, injectionManager, logSubsystemMissing);

        scheduleManager = subsystems.getSubsystem(IScheduleManager.class);

        if(scheduleManager != null){
            ISchedules schedulesHandle = scheduleManager.getScheduleHandle();
            if(schedulesHandle == null) {
                log.error("Schedule-Handle was null.");
                this.scheduleManager = null;
            }
            else {
                registerInstance(ISchedules.class, schedulesHandle);
            }
        }

        databaseManager = subsystems.getSubsystem(IDatabaseManager.class);
        resourcesManager = subsystems.getSubsystem(IResourcesManager.class);
    }

    private void startupSubsystems(){
        subsystems.startup();
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
        List<Class<?>> setClasses = plugz.getAnnotatedWith(Set.class);

        if(setClasses != null){
            log.debug("Instantiating [{}] @Set classes.", setClasses.size());
            for(Class<?> cls : setClasses){
                try {
                    log.trace("Register [{}] as @Set class...", cls.toString());
                    instanceManager.registerSetClass(cls);
                }
                catch (MagicInstanceException e) {
                    throw new MagicRuntimeException("Could not instantiate Service class: [" + cls.getName() + "].", e);
                }
            }
        }

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

        List<Class<?>> entityClasses = plugz.getAnnotatedWith(Entity.class);

        if(entityClasses != null){
            if(entityClasses.size() != 0 && databaseManager == null){
                log.error("No Database Manager available, so no @Entity classes can be handled!");
            }
            else {
                log.debug("Registering [{}] @Entity classes.", entityClasses.size());
                for(Class<?> cls : entityClasses){
                    try {
                        log.trace("Registering entity [{}]...", cls);
                        databaseManager.registerEntity(cls);
                    }
                    catch (DataBaseException e){
                        throw new MagicRuntimeException("Could not register Entity: [" + cls.getName() + "]!", e);
                    }
                }
            }
        }

        List<Class<?>> repoClasses = plugz.getAnnotatedWith(Repo.class);

        if(repoClasses != null){
            if(repoClasses.size() != 0 && databaseManager == null){
                log.error("No Database Manager available, so no @Repo classes can be handled!");
            }
            else {
                log.debug("Registering [{}] @Repo classes.", repoClasses.size());
                for(Class<?> cls : repoClasses){
                    Object repoImplementation;
                    try {
                        log.trace("Creating repository [{}]...", cls);
                        repoImplementation = databaseManager.registerAndImplementRepo(cls);
                    }
                    catch (DataBaseException e){
                        throw new MagicRuntimeException("Could not create repository from class: [" + cls.getName() + "].", e);
                    }

                    if(repoImplementation == null){
                        throw new MagicRuntimeException("Database Manager returned null implementation for repository class: [" + cls.getName() + "].");
                    }

                    URL fromUrl = Utils.getUrlFromClass(cls);

                    log.trace("Registering repository implementation [{}]...", repoImplementation);
                    registerInstance(instanceManager, fromUrl, cls, repoImplementation);
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
                        log.trace("Building resource [{}]...", cls);
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

    private Map<URL, List<ScheduledMethodConfig>> passScheduledMethods(){
        Map<URL, List<ScheduledMethodConfig>> configs = new HashMap<>(instanceManager.toScheduleMethods);
        instanceManager.toScheduleMethods.clear();

        if(!configs.isEmpty() && scheduleManager == null){
            log.error("No [schedule-manager] available, so no @Scheduled methods can be run.");
        }
        else {
            for (URL url : configs.keySet()) {
                for (ScheduledMethodConfig config : configs.get(url)) {
                    scheduleManager.scheduleRunnable(url, config);
                }
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

        if(scheduleManager != null) {
            log.debug("Starting Schedule Manager...");
            scheduleManager.start();
        }

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
        URL fromUrl = Utils.getUrlFromClass(instance.getClass());
        return registerInstance(manager, fromUrl, cls, instance);
    }

    private MagicEnvironment registerInstance(MagicInstanceManager manager, URL fromUrl, Class<?> cls, Object instance){
        try {
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
