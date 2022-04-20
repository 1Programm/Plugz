package com.programm.plugz.magic;

import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.auto.GetConfig;
import com.programm.plugz.api.auto.SetConfig;
import com.programm.plugz.api.lifecycle.LifecycleState;
import com.programm.plugz.inject.PlugzUrlClassScanner;
import com.programm.plugz.inject.ScanException;
import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Logger("Plugz")
public class MagicEnvironment {

    private static final boolean DEFAULT_ADD_SHUTDOWN_HOOK = true;

    public static MagicEnvironment Start() throws MagicSetupException {
        return Start(new String[0]);
    }

    public static MagicEnvironment Start(String... args) throws MagicSetupException {
        MagicEnvironment env = MagicEnvironmentBuilder.create(args);
        env.setup();
        env.startup();
        return env;
    }


    private final String basePackage;

    private final ProxyLogger log;
    private final PlugzUrlClassScanner scanner;
    private final ConfigurationManager configurations;
    private final ThreadPoolManager asyncManager;
    private final AnnotationChecker annocheck;
    private final MagicInstanceManager instanceManager;
    private final SubsystemManager subsystems;

    public MagicEnvironment(String... args){
        this("", args);
    }

    public MagicEnvironment(String basePackage, String... args){
        this.basePackage = basePackage;

        this.log = new ProxyLogger();
        this.scanner = new PlugzUrlClassScanner();
        this.scanner.setLogger(log);
        this.configurations = new ConfigurationManager(args);
        this.asyncManager = new ThreadPoolManager(log);
        this.annocheck = new AnnotationChecker();
        this.instanceManager = new MagicInstanceManager(log, configurations, asyncManager, annocheck);
        this.subsystems = new SubsystemManager(log, scanner, configurations, annocheck, instanceManager);

        setupAnnocheck();

        try {
            this.instanceManager.registerInstance(ILogger.class, log);
            this.instanceManager.registerInstance(PlugzConfig.class, configurations);
            this.instanceManager.registerInstance(IAsyncManager.class, asyncManager);
        }
        catch (MagicInstanceException e){
            throw new IllegalStateException("INVALID STATE: There should be no class waiting yet!", e);
        }
    }

    private void setupAnnocheck(){
        //SetConfig annotation can only be used inside classes annotated by Config
        this.annocheck.forClass(SetConfig.class).classAnnotations().whitelist().set(Config.class).seal();

        this.annocheck.forClass(Get.class).partnerAnnotations().blacklist().set(GetConfig.class);
    }

    public void setup() throws MagicSetupException {
        log.info("Setting up the environment with profile: [{}]", configurations.profile());

        log.debug("Initializing configurations from args and profile-resource...");
        try {
            configurations.init();
        }
        catch (MagicSetupException e){
            throw new MagicSetupException("Failed to initialize configuration manager.", e);
        }

        List<URL> collectedUrls;

        log.debug("Collecting urls to scan through...");
        try {
            collectedUrls = collectScanUrls();
        }
        catch (IOException e){
            throw new MagicSetupException("Failed to collect scan urls.", e);
        }

        log.debug("Starting config scan");
        try {
            scanner.addSearchClass(ISubsystem.class);
            scanner.addSearchAnnotation(Config.class);

            log.debug("Scanning through collected urls with a base package of [{}]...", basePackage);
            scanner.scan(collectedUrls, basePackage);
        }
        catch (ScanException e){
            throw new MagicSetupException("Config scan failed with a base package of [" + basePackage + "]!", e);
        }

        List<Class<?>> configClasses = scanner.getAnnotatedWith(Config.class);
        log.debug("Registering [{}] configuration classes", configClasses.size());
        for(Class<?> cls : configClasses){
            try {
                if(instanceManager.checkConfigNeeded(cls)) {
                    instanceManager.instantiate(cls);
                }
            }
            catch (MagicInstanceException e){
                throw new MagicSetupException("Failed to instantiate config class: [" + cls.getName() + "]!", e);
            }
        }

        try {
            instanceManager.checkWaitMap(false);
        }
        catch (MagicInstanceException e) {
            throw new MagicSetupException("Exception solving wait dependencies in the config phase!", e);
        }
        catch (MagicInstanceWaitException e){
            throw new MagicSetupException(e.getMessage());
        }

        asyncManager.init(configurations);

        try {
            scanner.addSearchAnnotation(Service.class);
            subsystems.prepare();
        }
        catch (MagicInstanceException e){
            throw new MagicSetupException("Failed to prepare subsystems!", e);
        }


        log.info("Starting main scan");
        try {
            log.debug("Scanning through collected urls with a base package of [{}]...", basePackage);
            scanner.scan(collectedUrls, basePackage);
        }
        catch (ScanException e){
            throw new MagicSetupException("Main scan failed with a base package of [" + basePackage + "]!", e);
        }


        List<Class<?>> serviceClasses = scanner.getAnnotatedWith(Service.class);
        log.debug("Registering [{}] service classes", serviceClasses.size());
        for(Class<?> cls : serviceClasses){
            try {
                instanceManager.instantiate(cls);
            }
            catch (MagicInstanceException e){
                throw new MagicSetupException("Failed to instantiate service class: [" + cls.getName() + "]!", e);
            }
        }

        log.debug("Setting up discovered classes from subsystems...");
        try {
            subsystems.setupFoundClasses();
        }
        catch (MagicInstanceException e){
            throw new MagicSetupException("Failed to set up discovered classes from subsystems!", e);
        }

        try {
            instanceManager.checkWaitMap(true);
        }
        catch (MagicInstanceException e) {
            throw new MagicSetupException("Exception solving wait dependencies in the service-setup phase!", e);
        }
        catch (MagicInstanceWaitException e){
            throw new MagicSetupException(e.getMessage());
        }

        try {
            instanceManager.callLifecycleMethods(LifecycleState.POST_SETUP);
        }
        catch (MagicInstanceException e){
            throw new MagicSetupException("Exception while calling " + LifecycleState.POST_SETUP + " methods!", e);
        }
    }

    public void startup() {
        log.info("Starting up the environment");

        if(configurations.getBooleanOrDefault("core.shutdownhook.enabled", DEFAULT_ADD_SHUTDOWN_HOOK)){
            log.debug("Adding shutdown-hook...");
            addShutdownHook();
        }

        try {
            instanceManager.callLifecycleMethods(LifecycleState.PRE_STARTUP);
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException(e);
        }

        try {
            subsystems.startup();
        }
        catch (MagicException e){
            throw new MagicRuntimeException(e);
        }

        try {
            instanceManager.callLifecycleMethods(LifecycleState.POST_STARTUP);
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException(e);
        }

        log.info("Environment started up!");
    }

    public void shutdown(){
        log.info("Shutting down the environment");
        try {
            instanceManager.callLifecycleMethods(LifecycleState.PRE_SHUTDOWN);
        }
        catch (MagicInstanceException e){
            log.error(e.getMessage());
            e.printStackTrace();
        }

        try {
            subsystems.shutdown();
        }
        catch (MagicException e){
            throw new MagicRuntimeException(e);
        }

        asyncManager.shutdown();

        try {
            instanceManager.callLifecycleMethods(LifecycleState.POST_SHUTDOWN);
        }
        catch (MagicInstanceException e){
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }






    private List<URL> collectScanUrls() throws IOException {
        List<URL> searchUrls = new ArrayList<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> urls = cl.getResources("");

        while(urls.hasMoreElements()) {
            URL url = urls.nextElement();
            log.trace("# Found [{}].", url);
            searchUrls.add(url);
        }

        return searchUrls;
    }

    private void addShutdownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.debug("Caught shutdown...");
            shutdown();
        }, "Shutdown Hook"));
    }






    public void setLogger(ILogger log){
        this.log.setLogger(log);
    }

}
