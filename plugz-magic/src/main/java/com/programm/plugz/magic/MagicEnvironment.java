package com.programm.plugz.magic;

import com.programm.plugz.api.*;
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

    public static MagicEnvironment Start() throws MagicSetupException {
        return Start(new String[0]);
    }

    public static MagicEnvironment Start(String... args) throws MagicSetupException {
        MagicEnvironment env =  MagicEnvironmentBuilder.create(args);
        env.setup();
        env.startup();
        return env;
    }


    private final String basePackage;
    private final String[] initialArgs;

    private final ProxyLogger log;
    private final PlugzUrlClassScanner scanner;
    private final ConfigurationManager configurations;
    private final MagicInstanceManager instanceManager;

    public MagicEnvironment(String... args){
        this("", args);
    }

    public MagicEnvironment(String basePackage, String... args){
        this.basePackage = basePackage;
        this.initialArgs = args;

        this.log = new ProxyLogger();
        this.scanner = new PlugzUrlClassScanner();
        this.scanner.setLogger(log);
        this.configurations = new ConfigurationManager();
        this.instanceManager = new MagicInstanceManager(configurations, log);

        try {
            this.instanceManager.registerInstance(ILogger.class, log);
            this.instanceManager.registerInstance(PlugzConfig.class, configurations);
        }
        catch (MagicInstanceException e){
            throw new IllegalStateException("INVALID STATE: There should be no class waiting yet!", e);
        }

        scanner.addSearchClass(ISubsystem.class);
        scanner.addSearchAnnotation(Config.class);
        scanner.addSearchAnnotation(Service.class);
    }

    public void setup() throws MagicSetupException {
        log.info("[[[ Configuration Phase ]]]");

        log.debug("Initializing configurations from args and profile-resource...");
        try {
            configurations.init(initialArgs);
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

        log.debug("Scanning through collected urls with a base package of [{}]...", basePackage);
        try {
            scanner.scan(collectedUrls, basePackage);
        }
        catch (ScanException e){
            throw new MagicSetupException("Failed to scan through collected urls and a base package [" + basePackage + "]!", e);
        }

        List<Class<?>> configClasses = scanner.getAnnotatedWith(Config.class);
        log.debug("Registering [{}] configuration classes", configClasses.size());
        for(Class<?> cls : configClasses){
            try {
                instanceManager.checkConfigNeeded(cls);
                instanceManager.instantiate(cls);
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
        try {
            instanceManager.callLifecycleMethods(LifecycleState.PRE_STARTUP);
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException("Exception while calling " + LifecycleState.PRE_STARTUP + " methods!", e);
        }


    }

    public void shutdown(){

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






    public void setLogger(ILogger log){
        this.log.setLogger(log);
    }

}
