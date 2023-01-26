package com.programm.plugz.magic;

import com.programm.ioutils.io.api.IOutput;
import com.programm.ioutils.log.api.IConfigurableLogger;
import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.ioutils.log.api.LoggerConfigException;
import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.auto.GetConfig;
import com.programm.plugz.api.auto.SetConfig;
import com.programm.plugz.api.condition.IConditionTester;
import com.programm.plugz.api.instance.IInstanceManager;
import com.programm.plugz.api.lifecycle.LifecycleState;
import com.programm.plugz.inject.ScanCriteria;
import com.programm.plugz.inject.ScanException;
import com.programm.plugz.inject.UrlClassScanner;

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
    private static final String CONF_LOGGER_FORMAT_DEFAULT = "[$TIME] [%5<($LVL)] [%30>($LOG?{$CLS.$MET})]: $MSG";
    private static final String CONF_LOGGER_OUT_NAME = "log.out";
    private static final String CONF_LOGGER_OUT_DEFAULT = "com.programm.plugz.magic.LoggerDefaultConsoleOut";
    private static final String CONF_LOGGER_LOG_STACKTRACE_NAME = "log.stacktrace";
    private static final String CONF_LOGGER_LOG_STACKTRACE_DEFAULT = "true";


    public static MagicEnvironment Start() throws MagicSetupException {
        return Start(new String[0]);
    }

    public static MagicEnvironment Start(String... args) throws MagicSetupException {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String _className = stack[stack.length - 1].getClassName();
        Class<?> executingClass;
        try {
            executingClass = Class.forName(_className);
        }
        catch (ClassNotFoundException e){
            throw new MagicSetupException("Failed to get executing class [" + _className + "]!", e);
        }

        URL executingClassUrl = executingClass.getResource("/");


        MagicEnvironment env = new MagicEnvironment(args);
        env.setExecutingUrl(executingClassUrl);

        MagicRuntime runtimeSettings = executingClass.getAnnotation(MagicRuntime.class);
        if(runtimeSettings != null){
            env.componentScanPath(runtimeSettings.scanPath());
        }

        env.setup();
        env.startup();
        return env;
    }


    private final LoggerProxy log;
    private final UrlClassScanner scanner;
    private final ConfigurationManager configurations;
    private final MagicContext context;

    private final ThreadPoolManager asyncManager;
    private final AnnotationChecker annocheck;
    private final ConditionTesterProxy conditionTester;
    private final MagicInstanceManager instanceManager;
    private final SubsystemManager subsystems;


    private long setupBeginTime;
    private URL executingClassUrl;
    private String componentScanPath = null;

    public MagicEnvironment(String... args){
        this.log = new LoggerProxy();
        this.scanner = new UrlClassScanner();
        this.scanner.setLogger(log);
        this.configurations = new ConfigurationManager(log, args);
        this.context = new MagicContext(configurations);

        logBanner();

        initDefaultConfigs();

        this.asyncManager = new ThreadPoolManager(log);
        this.annocheck = new AnnotationChecker();
        this.conditionTester = new ConditionTesterProxy();
        context.conditionTester = conditionTester;
        this.instanceManager = new MagicInstanceManager(log, configurations, asyncManager, annocheck, context);
        context.instanceManager = this.instanceManager;
        this.subsystems = new SubsystemManager(log, scanner, conditionTester, annocheck, instanceManager);

        setupAnnocheck();

        try {
            this.instanceManager.registerInstance(IInstanceManager.class, instanceManager);
            this.instanceManager.registerInstance(ILogger.class, log);
            this.instanceManager.registerInstance(PlugzConfig.class, configurations);
            this.instanceManager.registerInstance(IAsyncManager.class, asyncManager);
        }
        catch (MagicInstanceException e){
            throw new IllegalStateException("INVALID STATE: There should be no class waiting yet!", e);
        }
    }

    private void logBanner(){
        log.info("<==========> Plugz Version 2 <==========>");
    }

    private void initDefaultConfigs(){
        log.debug("Setting up default configurations for the environment.");
        configurations.registerDefaultConfiguration(CONF_ADD_SHUTDOWN_HOOK_NAME, CONF_ADD_SHUTDOWN_HOOK_DEFAULT);

        configurations.registerDefaultConfiguration(CONF_LOGGER_IMPL_CLASS_NAME, null);
        configurations.registerDefaultConfiguration(CONF_LOGGER_LEVEL_NAME, CONF_LOGGER_LEVEL_DEFAULT);
        configurations.registerDefaultConfiguration(CONF_LOGGER_FORMAT_NAME, CONF_LOGGER_FORMAT_DEFAULT);
        configurations.registerDefaultConfiguration(CONF_LOGGER_OUT_NAME, CONF_LOGGER_OUT_DEFAULT);
        configurations.registerDefaultConfiguration(CONF_LOGGER_LOG_STACKTRACE_NAME, CONF_LOGGER_LOG_STACKTRACE_DEFAULT);
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
                log.logger = new LoggerFallback().config("output", new LoggerDefaultConsoleOut());
                log.passStoredLogs();
            }

            throw e;
        }
        long setupEndTime = System.currentTimeMillis();
        log.debug("Setting up the environment took [{}] milliseconds!", setupEndTime - setupBeginTime);
    }

    private void doSetup() throws MagicSetupException {
        log.info("Setting up the environment with profile: [{}]", configurations.profile());

        try {
            log.debug("Initializing configurations from profile-resource...");
            configurations.initProfileConfig();
        }
        catch (MagicSetupException e){
            throw new MagicSetupException("Failed to initialize configuration manager.", e);
        }

        List<URL> collectedUrls;
        try {
            log.debug("Collecting context urls...");
            collectedUrls = collectScanUrls();
            context.scanUrls = new ArrayList<>(collectedUrls);
        }
        catch (IOException e){
            throw new MagicSetupException("Failed to collect scan urls.", e);
        }

        logLifecycleState(LifecycleState.PRE_SETUP);
        List<Class<?>> subsystemsClasses = new ArrayList<>();
        List<Class<?>> loggerImplementations = new ArrayList<>();
        List<Class<?>> configAnnotatedClasses = new ArrayList<>();
        List<Class<?>> conditionTesterClasses = new ArrayList<>();

        try {
            log.debug("Starting config scan");
            log.debug("Scanning through {} collected urls ...", collectedUrls.size());

            if(componentScanPath != null) scanner.entryPoint(executingClassUrl, componentScanPath);

            scanner.forUrls(collectedUrls)
                    .withCriteria(ScanCriteria.createOnSuccessCollect("Subsystem", subsystemsClasses)
                            .classImplements(ISubsystem.class))
                    .withCriteria(ScanCriteria.createOnSuccessCollect("Logger implementation", loggerImplementations)
                            .blacklistPackages("com.programm.ioutils.log.api")
                            .blacklistClasses(LoggerFallback.class, LoggerProxy.class)
                            .classImplements(ILogger.class))
                    .withCriteria(ScanCriteria.createOnSuccessCollect("Config class", configAnnotatedClasses)
                            .classAnnotatedWith(Config.class))
                    .withCriteria(ScanCriteria.createOnSuccessCollect("Condition Tester", conditionTesterClasses)
                            .blacklistClasses(ConditionTesterProxy.class, SimpleConditionTester.class)
                            .classImplements(IConditionTester.class))
                    .scan();

            scanner.clearConfig();
            context.subsystems = new ArrayList<>(subsystemsClasses);
        }
        catch (ScanException e){
            throw new MagicSetupException("Config scan failed!", e);
        }

        log.debug("Setting up condition tester...");
        if(conditionTesterClasses.size() >= 1){
            Class<?> conditionTesterClass = conditionTesterClasses.get(0);
            if(conditionTesterClasses.size() > 1) log.warn("Multiple implementations found for [{}]! Using [{}].", IConditionTester.class, conditionTesterClass);

            try {
                instanceManager.instantiate(conditionTesterClass, instance -> {
                    conditionTester.tester = (IConditionTester) instance;
                });
            }
            catch (MagicInstanceException e){
                throw new MagicSetupException("Failed to instantiate condition tester [" + conditionTesterClass + "]", e);
            }
        }
        else {
            log.debug("No condition tester implementation found. Using default implementation.");
            conditionTester.tester = new SimpleConditionTester();
        }
        log.info("Using Condition Tester [{}].", conditionTester.tester.getClass().getName());

        log.debug("Registering [{}] configuration classes", configAnnotatedClasses.size());
        for(Class<?> cls : configAnnotatedClasses){
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


        log.debug("Initializing configurations from args...");
        configurations.initArgs();


        log.debug("Searching for logger implementation...");
        findLoggerImplementation(loggerImplementations);

        log.trace("Passing stored logs to logger implementation as now all configurations are set.");
        log.passStoredLogs();

        log.debug("Setting up [Async-Manager].");
        asyncManager.init(configurations);


        try {
            subsystems.prepare(subsystemsClasses);
        }
        catch (MagicInstanceException e){
            throw new MagicSetupException("Failed to prepare subsystems!", e);
        }

        List<Class<?>> serviceClasses = new ArrayList<>();

        try {
            log.debug("Starting main scan");
            log.debug("Scanning through {} collected urls ...", collectedUrls.size());

            if(componentScanPath != null) scanner.entryPoint(executingClassUrl, componentScanPath);

            scanner.forUrls(collectedUrls)
                    .withCriteria(ScanCriteria.createOnSuccessCollect("Service class", serviceClasses).classAnnotatedWith(Service.class))
                    .scan();

            scanner.clearConfig();
        }
        catch (ScanException e){
            throw new MagicSetupException("Main scan failed!", e);
        }


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
            log.debug("Setting up discovered classes from subsystems...");
            subsystems.setupFoundClasses();
        }
        catch (MagicInstanceException e){
            throw new MagicSetupException("Failed to set up discovered classes from subsystems!", e);
        }

        try {
            logLifecycleState(LifecycleState.POST_SETUP);
            instanceManager.callLifecycleMethods(LifecycleState.POST_SETUP);
        }
        catch (MagicInstanceException e){
            throw new MagicSetupException("Exception while calling " + LifecycleState.POST_SETUP + " methods!", e);
        }
    }

    public void startup() {
        long startBeginTime = System.currentTimeMillis();
        log.info("Starting up the environment");

        if(configurations.getBoolOrError(CONF_ADD_SHUTDOWN_HOOK_NAME, MagicRuntimeException::new)){
            log.debug("Adding shutdown-hook...");
            addShutdownHook();
        }

        try {
            logLifecycleState(LifecycleState.PRE_STARTUP);
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
            instanceManager.checkWaitMap(true);
        }
        catch (MagicInstanceException e) {
            throw new MagicRuntimeException("Exception solving wait dependencies in the service-setup phase!", e);
        }
        catch (MagicInstanceWaitException e){
            throw new MagicRuntimeException(e.getMessage());
        }

        try {
            logLifecycleState(LifecycleState.POST_STARTUP);
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
            logLifecycleState(LifecycleState.PRE_SHUTDOWN);
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
            logLifecycleState(LifecycleState.POST_SHUTDOWN);
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
//                if(loggerImplCls == LoggerFallback.class || loggerImplCls == LoggerProxy.class) continue;
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
            String _logLevel = configurations.getOrError(CONF_LOGGER_LEVEL_NAME, MagicSetupException::new);
            int logLevel;
            try {
                logLevel = Integer.parseInt(_logLevel);
            }
            catch (NumberFormatException e) {
                logLevel = ILogger.fromString(_logLevel);
            }

            String logFormat = configurations.get(CONF_LOGGER_FORMAT_NAME);
            String _logOut = configurations.get(CONF_LOGGER_OUT_NAME);
            boolean doLogStacktrace = configurations.getBool(CONF_LOGGER_LOG_STACKTRACE_NAME);

            try {
                configurableLogger.config("level", logLevel);
                if(logFormat != null) configurableLogger.config("format", logFormat);
                configurableLogger.config("printStacktraceForExceptions", doLogStacktrace);

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

                    configurableLogger.config("output", logOut);
                }

                for(Map.Entry<String, Object> entry : configurations.configValues.entrySet()){
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if(value == null) continue;

                    if(key.startsWith("log.pkg[") && key.endsWith("]")){
                        String pkgName = key.substring("log.pkg[".length(), key.length() - 1);
                        String _value = value.toString();

                        int pkgLevel;
                        try {
                            pkgLevel = Integer.parseInt(_value);
                        }
                        catch (NumberFormatException e) {
                            pkgLevel = ILogger.fromString(_value);
                        }

                        configurableLogger.config("packageLevel", pkgName, pkgLevel);
                    }
                    else if(key.startsWith("log.name[") && key.endsWith("]")){
                        String logName = key.substring("log.name[".length(), key.length() - 1);
                        String _value = value.toString();

                        int logNameLevel;
                        try {
                            logNameLevel = Integer.parseInt(_value);
                        }
                        catch (NumberFormatException e) {
                            logNameLevel = ILogger.fromString(_value);
                        }

                        configurableLogger.config("logNameLevel", logName, logNameLevel);
                    }
                }
            }
            catch (LoggerConfigException e){
                throw new MagicSetupException("Failed to configure the provided logger!", e);
            }
        }

        log.setLogger(loggerImplementation);
    }

    private void logLifecycleState(LifecycleState state){
        String _state = state.toString().toUpperCase();
        int numEquals = 50 - _state.length();
        int half = numEquals / 2;
        boolean even = numEquals % 2 == 0;
        log.debug("=".repeat(half) + "[ " + _state + " ]" + "=".repeat(even ? half : half + 1));
    }

    private void addShutdownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.debug("Caught shutdown...");
            shutdown();
        }, "Shutdown Hook"));
    }



    public void setExecutingUrl(URL executingURL){
        this.executingClassUrl = executingURL;
    }

    public void componentScanPath(String path) {
        this.componentScanPath = path;
    }

    public void setLogger(ILogger log){
        this.log.setLogger(log);
    }

    public void setConditionTester(IConditionTester conditionTester){
        this.conditionTester.tester = conditionTester;
    }

    public void setInstance(Class<?> cls, Object instance){
        try {
            instanceManager.registerInstance(cls, instance);
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException(e);
        }
    }

    public <T> T getInstance(Class<T> cls){
        try {
            return instanceManager.getInstance(cls);
        }
        catch (MagicInstanceException e){
            throw new MagicRuntimeException(e);
        }
    }

}
