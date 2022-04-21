package com.programm.plugz.magic;

import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.auto.GetConfig;
import com.programm.plugz.api.auto.SetConfig;
import com.programm.plugz.api.lifecycle.LifecycleState;
import com.programm.plugz.inject.PlugzUrlClassScanner;
import com.programm.plugz.inject.ScanException;
import com.programm.projects.ioutils.log.api.out.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Logger("Plugz")
public class MagicEnvironment {

    private static final String CONF_ADD_SHUTDOWN_HOOK_NAME = "core.shutdownhook.enabled";
    private static final boolean CONF_ADD_SHUTDOWN_HOOK_DEFAULT = true;

    private static final String CONF_LOGGER_IMPL_CLASS_NAME = "log.implementation";
    private static final String CONF_LOGGER_LEVEL_NAME = "log.level";
    private static final String CONF_LOGGER_LEVEL_DEFAULT = "INFO";
    private static final String CONF_LOGGER_FORMAT_NAME = "log.format";
    private static final String CONF_LOGGER_FORMAT_DEFAULT = "[%5<($LVL)] [%30>($LOG?{$CLS.$MET})]: $MSG";
    private static final String CONF_LOGGER_OUT_NAME = "log.out";
    private static final String CONF_LOGGER_OUT_DEFAULT = "com.programm.plugz.magic.LoggerDefaultConsoleOut";


    public static MagicEnvironment Start() throws MagicSetupException {
        return Start(new String[0]);
    }

    public static MagicEnvironment Start(String... args) throws MagicSetupException {
        MagicEnvironment env = new MagicEnvironment(args);
        env.setup();
        env.startup();
        return env;
    }


    private final String basePackage;

    private final LoggerProxy log;
    private final PlugzUrlClassScanner scanner;
    private final ConfigurationManager configurations;
    private final ThreadPoolManager asyncManager;
    private final AnnotationChecker annocheck;
    private final MagicInstanceManager instanceManager;
    private final SubsystemManager subsystems;

    private long setupBeginTime;

    public MagicEnvironment(String... args){
        this("", args);
    }

    public MagicEnvironment(String basePackage, String... args){
        this.basePackage = basePackage;

        this.log = new LoggerProxy();
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
        this.setupBeginTime = System.currentTimeMillis();
        try {
            doSetup();
        }
        catch (MagicSetupException e){
            if(log.logger == null){
                log.logger = new LoggerFallback().output(new LoggerDefaultConsoleOut());
                log.passStoredLogs();
            }

            throw e;
        }
        long setupEndTime = System.currentTimeMillis();
        log.debug("Setting up the environment took [{}] milliseconds!", setupEndTime - setupBeginTime);
    }

    private void doSetup() throws MagicSetupException {
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
            scanner.addSearchClass(ILogger.class);
            scanner.addSearchAnnotation(Config.class);

            log.debug("Scanning through {} collected urls with a base package of [{}]...", collectedUrls.size(), basePackage);
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

        log.debug("Searching for logger implementation...");
        findLoggerImplementation(scanner.getImplementing(ILogger.class));
        log.passStoredLogs();

        asyncManager.init(configurations);


        try {
            scanner.addSearchAnnotation(Service.class);
            subsystems.prepare();
        }
        catch (MagicInstanceException e){
            throw new MagicSetupException("Failed to prepare subsystems!", e);
        }


        log.debug("Starting main scan");
        try {
            log.debug("Scanning through {} collected urls with a base package of [{}]...", collectedUrls.size(), basePackage);
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
        long startBeginTime = System.currentTimeMillis();
        log.info("Starting up the environment");

        if(configurations.getBooleanOrDefault(CONF_ADD_SHUTDOWN_HOOK_NAME, CONF_ADD_SHUTDOWN_HOOK_DEFAULT)){
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

        long startEndTime = System.currentTimeMillis();
        log.debug("Only starting up the environment took [{}] milliseconds!", startEndTime - startBeginTime);
        log.info("Environment started up in [{}] milliseconds!", startEndTime - setupBeginTime);
    }

    public void shutdown(){
        long shutdownBeginTime = System.currentTimeMillis();
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

        long shutdownEndTime = System.currentTimeMillis();
        log.debug("Shutting down the environment took [{}] milliseconds!", shutdownEndTime - shutdownBeginTime);
    }






    private List<URL> collectScanUrls() throws IOException {
        List<URL> searchUrls = new ArrayList<>();

        String[] searchUrlsAsString = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
        for(String searchUrlAsString : searchUrlsAsString){
            try {
                URL url = Paths.get(searchUrlAsString).toAbsolutePath().toUri().toURL();
                log.debug("# Found [{}].", url);
                searchUrls.add(url);
            }
            catch (MalformedURLException e){
                log.warn("Malformed url [{}]: {}", searchUrlsAsString, e.getMessage());
            }
        }

        return searchUrls;
    }

    private void findLoggerImplementation(List<Class<?>> loggerImplementations) throws MagicSetupException {
        Class<?> loggerImplementationClass;
        String loggerImplementationClassName = configurations.get(CONF_LOGGER_IMPL_CLASS_NAME);

        if(loggerImplementationClassName != null){
            try {
                loggerImplementationClass = Class.forName(loggerImplementationClassName);
            }
            catch (ClassNotFoundException e){
                throw new MagicSetupException("Could not find specified logger implementation for class name: [" + loggerImplementationClassName + "]!", e);
            }

            if(!ILogger.class.isAssignableFrom(loggerImplementationClass)) throw new MagicSetupException("The provided logger implementation [" + loggerImplementationClassName + "] does not implement the ILogger interface!");

            log.debug("Using provided logger implementation from config.");
        }
        else {
            loggerImplementationClass = null;
            for(Class<?> loggerImplCls : loggerImplementations) {
                if(loggerImplCls == LoggerFallback.class || loggerImplCls == LoggerProxy.class) continue;
                loggerImplementationClass = loggerImplCls;
                break;
            }

            if(loggerImplementationClass != null) {
                log.debug("Using logger found through class scan.");
            }
        }

        ILogger loggerImplementation;
        if(loggerImplementationClass == null){
            log.info("No logger implementation provided. Using Logger fallback.");
            loggerImplementation = new LoggerFallback();
        }
        else {
            try {
                loggerImplementation = (ILogger) MagicInstanceManager.createFromEmptyConstructor(loggerImplementationClass);
            }
            catch (NoSuchMethodException e){
                throw new MagicSetupException("The provided logger implementation [" + loggerImplementationClassName + "] does not have an empty constructor!", e);
            }
            catch (InvocationTargetException | InstantiationException e) {
                throw new MagicSetupException("Failed to instantiate the provided logger implementation [" + loggerImplementationClassName + "]!", e);
            }
            catch (IllegalAccessException e) {
                throw new MagicSetupException("The empty constructor for the provided logger implementation [" + loggerImplementationClassName + "] cannot be accessed!", e);
            }

            log.info("Using Logger [{}]", loggerImplementationClass.getName());
        }

        if(loggerImplementation instanceof IConfigurableLogger configurableLogger){
            String _logLevel = configurations.getOrDefault(CONF_LOGGER_LEVEL_NAME, CONF_LOGGER_LEVEL_DEFAULT);
            int logLevel;
            try {
                logLevel = Integer.parseInt(_logLevel);
            }
            catch (NumberFormatException e) {
                logLevel = ILogger.fromString(_logLevel);
            }

            String logFormat = configurations.getOrDefault(CONF_LOGGER_FORMAT_NAME, CONF_LOGGER_FORMAT_DEFAULT);
            String _logOut = configurations.getOrDefault(CONF_LOGGER_OUT_NAME, CONF_LOGGER_OUT_DEFAULT);

            try {
                configurableLogger.level(logLevel);
                configurableLogger.format(logFormat);

                if(!_logOut.isEmpty()){
                    IOutput logOut;
                    try {
                        logOut = (IOutput) MagicInstanceManager.createFromEmptyConstructor(_logOut);
                    }
                    catch (ClassNotFoundException e){
                        throw new MagicSetupException("Could not find specified logger - output implementation for class name: [" + _logOut + "]!", e);
                    }
                    catch (NoSuchMethodException e){
                        throw new MagicSetupException("The provided logger implementation [" + loggerImplementationClassName + "] does not have an empty constructor!", e);
                    }
                    catch (InvocationTargetException | InstantiationException e) {
                        throw new MagicSetupException("Failed to instantiate the provided logger implementation [" + loggerImplementationClassName + "]!", e);
                    }
                    catch (IllegalAccessException e) {
                        throw new MagicSetupException("The empty constructor for the provided logger implementation [" + loggerImplementationClassName + "] cannot be accessed!", e);
                    }

                    configurableLogger.output(logOut);
                }

                for(Map.Entry<String, Object> entry : configurations.configValues.entrySet()){
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if(key.startsWith("log.pkg[") && key.endsWith("]")){
                        if(value == null) continue;

                        String pkgName = key.substring("log.pkg[".length(), key.length() - 1);
                        String _value = value.toString();

                        int pkgLevel;
                        try {
                            pkgLevel = Integer.parseInt(_value);
                        }
                        catch (NumberFormatException e) {
                            pkgLevel = ILogger.fromString(_value);
                        }

                        configurableLogger.packageLevel(pkgName, pkgLevel);
                    }
                }
            }
            catch (LoggerConfigException e){
                throw new MagicSetupException("Failed to configure the provided logger!", e);
            }
        }

        log.setLogger(loggerImplementation);
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
