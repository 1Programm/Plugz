package com.programm.plugz.magic;

import com.programm.plugz.annocheck.AnnotationCheckException;
import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.auto.SetConfig;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.auto.GetConfig;
import com.programm.plugz.api.auto.Set;
import com.programm.plugz.api.instance.*;
import com.programm.plugz.api.lifecycle.LifecycleState;
import com.programm.plugz.api.utils.ValueUtils;
import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

@Logger("Instance Manager")
@RequiredArgsConstructor
public class MagicInstanceManager implements IInstanceManager {

    private static String getMethodString(Method method){
        return "Method#" + method.getDeclaringClass().getName() + "#" + method.getName();
    }

    private static String getConstructorString(Constructor<?> constructor){
        return "Constructor#" + constructor.getDeclaringClass().getName() + "#" + Arrays.toString(constructor.getParameterTypes());
    }

    private interface InstanceProvider {
        Object get() throws MagicInstanceException;
    }

    @RequiredArgsConstructor
    private static class ConstantInstance implements InstanceProvider {

        private final Object instance;

        @Override
        public Object get() {
            return instance;
        }
    }

    private interface MagicWire {
        String name();
        boolean required();
        Class<?> type();
        void accept(Object o) throws MagicInstanceException;
    }

    private class MethodInstanceProvider implements InstanceProvider {
        private final Object instance;
        private final Method method;
        private final Object[] params;
        private final boolean persist;

        private int missingParams;
        private Object persistedObject;
        private boolean registered;

        public MethodInstanceProvider(Object instance, Method method, Object[] params, boolean persist) {
            this.instance = instance;
            this.method = method;
            this.params = params;
            this.persist = persist;
            this.missingParams = params.length;
        }

        public void putParam(int pos, Object param) {
            params[pos] = param;
            missingParams--;
        }

        @Override
        public Object get() throws MagicInstanceException {
            if(missingParams != 0) return null;

            if(persist){
                if(persistedObject == null){
                    persistedObject = invokeMethod(instance, method, params);
                }

                return persistedObject;
            }

            return invokeMethod(instance, method, params);
        }
    }

    @RequiredArgsConstructor
    private class MissingFieldWire implements MagicWire {
        private final Object instance;
        private final Field field;
        private final boolean required;

        @Override
        public String name() {
            return field.toString();
        }

        @Override
        public boolean required() {
            return required;
        }

        @Override
        public Class<?> type() {
            return field.getType();
        }

        @Override
        public void accept(Object o) {
            setField(field, instance, o);
        }
    }

    private class MissingParamsConstructor {
        private final Class<?> cls;
        private final Constructor<?> constructor;
        private final Object[] params;
        private final SetupFunction setupFunction;

        private int missingParams;

        public MissingParamsConstructor(Class<?> cls, Constructor<?> constructor, Object[] params, SetupFunction setupFunction) {
            this.cls = cls;
            this.constructor = constructor;
            this.params = params;
            this.setupFunction = setupFunction;
            this.missingParams = params.length;
        }

        public void putParam(int pos, Object param) throws MagicInstanceException {
            params[pos] = param;
            missingParams--;
            if(missingParams == 0) invoke();
        }

        public void invoke() throws MagicInstanceException {
            Object instance = invokeConstructor(constructor, params);
            setupFunction.setup(cls, instance);
        }
    }

    private class MissingParamsMethod {
        private final Object instance;
        private final Method method;
        private final Object[] params;

        private int missingParams;

        public MissingParamsMethod(Object instance, Method method, Object[] params) {
            this.instance = instance;
            this.method = method;
            this.params = params;
            this.missingParams = params.length;
        }

        public void putParam(int pos, Object param) throws MagicInstanceException {
            params[pos] = param;
            missingParams--;

            if(missingParams == 0){
                invokeMethod(instance, method, params);
            }
        }

    }

    @RequiredArgsConstructor
    private class MagicMethodImpl implements MagicMethod {
        private final Object instance;
        private final Method method;
        private final Consumer<Object> waitedResponse;

        public Object invoke(Object... args) throws MagicInstanceException {
            return tryInvokeMethod(instance, method, canWait, waitedResponse, args);
        }

        @Override
        public String toString() {
            return method.toString();
        }
    }

    private interface InstanceFunction <T> {
        Object apply(Class<?> cls, T t) throws MagicInstanceException;
    }

    private interface RegisterInstanceFunction <T> {
        void register(Class<?> cls, T t, InstanceProvider provider) throws MagicInstanceException;
    }

    private interface SetupFunction {
        void setup(Class<?> cls, Object instance) throws MagicInstanceException;
    }










    private static void handleFieldAnnotatedWithGet(Get annotation, Object instance, Field field, IInstanceManager manager, PlugzConfig config) throws MagicInstanceException {
        Class<?> type = field.getType();
        Object value = manager.getInstance(type);

        if(value != null){
            manager.setField(field, instance, value);
        }
        else if(manager.canWait()){
            manager.waitForField(type, instance, field, annotation.required());
        }
        else {
            if(annotation.required()) throw new MagicInstanceException("Could not get nor wait for parameter of type: [" + type + "]!");
            Object defaultValue = ValueUtils.getDefaultValue(type);
            manager.setField(field, instance, defaultValue);
        }
    }

    private static void handleFieldAnnotatedWithGetConfig(GetConfig annotation, Object instance, Field field, IInstanceManager manager, PlugzConfig config) throws MagicInstanceException {
        Class<?> type = field.getType();
        Object value = config.get(annotation.value());

        if(value == null) {
            value = ValueUtils.getDefaultValue(type);
        }

        manager.setField(field, instance, value);
    }

    private static void handleFieldAnnotatedWithSetConfig(SetConfig annotation, Object instance, Field field, IInstanceManager manager, PlugzConfig config) throws MagicInstanceException {
        int mods = field.getModifiers();
        if(!Modifier.isFinal(mods)) throw new MagicInstanceException("Field [" + field + "] must be final to be used as a config value!");
        boolean isStatic = Modifier.isStatic(mods);

        SetConfig configValueAnnotation = field.getAnnotation(SetConfig.class);
        String key = configValueAnnotation.value();
        Object value = manager.getField(field, isStatic ? null : instance);

        config.registerConfiguration(key, value);
    }

















    private final ILogger log;
    private final ConfigurationManager plugzConfig;
    private final ThreadPoolManager asyncManager;
    private final AnnotationChecker annocheck;

    private final Map<Class<? extends Annotation>, IAnnotatedFieldSetup<?>> annotatedFieldSetupMap = new HashMap<>();
    {
        registerFieldSetup(Get.class, MagicInstanceManager::handleFieldAnnotatedWithGet);
        registerFieldSetup(GetConfig.class, MagicInstanceManager::handleFieldAnnotatedWithGetConfig);
        registerFieldSetup(SetConfig.class, MagicInstanceManager::handleFieldAnnotatedWithSetConfig);
    }

    private final Map<Class<? extends Annotation>, IAnnotatedMethodSetup<?>> annotatedMethodSetupMap = new HashMap<>();

    private final Map<Class<?>, InstanceProvider> instanceMap = new HashMap<>();
    private final Map<Class<?>, List<MagicWire>> waitMap = new HashMap<>();
    private boolean canWait = true;

    private final Map<LifecycleState, List<MagicMethod>> lifecycleMethods = new HashMap<>();






















    public boolean checkConfigNeeded(Class<?> cls) {
        Config configAnnotation = cls.getAnnotation(Config.class);
        String specificProfile = configAnnotation.value();
        if(!specificProfile.isEmpty()){
            if((specificProfile.equals("main") && plugzConfig.profile() != null)
                    || (!plugzConfig.profile().equals(specificProfile))) {
                log.debug("Config class [{}] will be ignored as it does not match the active profile. [" + specificProfile + "] != [" + plugzConfig.profile() + "]", cls);
                return false;
            }
        }

        return true;
    }

    @Override
    public void instantiate(Class<?> cls) throws MagicInstanceException {
        instantiate(cls, null);
    }

    @Override
    public void instantiate(Class<?> cls, MagicConsumer<Object> callback) throws MagicInstanceException {
        try {
            annocheck.checkAllDeclared(cls);
        }
        catch (AnnotationCheckException e){
            throw new MagicInstanceException("Annotation checks failed!", e);
        }

        try {
            createInstanceFromConstructor(cls, (c, inst) -> {
                setupClass(c, inst);
                try {
                    if (callback != null) callback.accept(inst);
                }
                catch (MagicException e){
                    throw new MagicInstanceException(e);
                }
            });
        }
        catch (MagicInstanceException e){
            throw new MagicInstanceException("Could not create instance of class: [" + cls.getName() + "] from constructors!", e);
        }
    }

    private void setupClass(Class<?> cls, Object instance) throws MagicInstanceException {
        tryMagicFields(cls, instance);
        tryMagicMethods(cls, instance);

        registerInstance(cls, instance);
    }

    public void checkWaitMap(boolean disableWaiting) throws MagicInstanceException, MagicInstanceWaitException {
        if(!waitMap.isEmpty()){
            List<MagicWire> acceptDefaultWires = new ArrayList<>();

            for(Class<?> cls : waitMap.keySet()){
                List<MagicWire> waitingWires = waitMap.get(cls);
                for(MagicWire wire : waitingWires){
                    if(!wire.required()){
                        acceptDefaultWires.add(wire);
                    }
                }
            }

            for(MagicWire wire : acceptDefaultWires){
                Class<?> type = wire.type();
                Object defaultVal = ValueUtils.getDefaultValue(type);
                wire.accept(defaultVal);
            }

            if(!waitMap.isEmpty()){
                String errString = buildWaitError(waitMap);
                throw new MagicInstanceWaitException("Required open wires found:" + errString);
            }
        }

        if(disableWaiting){
            canWait = false;
        }
    }

    private String buildWaitError(Map<Class<?>, List<MagicWire>> openWiresMap) {
        StringBuilder sb = new StringBuilder();

        for(Class<?> cls : openWiresMap.keySet()){
            List<MagicWire> wires = openWiresMap.get(cls);

            sb.append("\n").append("Waiting for [").append(cls.getName()).append("]: ");
            for (MagicWire wire : wires) {
                sb.append("\n\t").append(wire.name());
            }
        }

        return sb.toString();
    }

    private void waitFor(Class<?> cls, MagicWire mw){
        waitMap.computeIfAbsent(cls, c -> new ArrayList<>()).add(mw);
    }

    @Override
    public void waitForField(Class<?> type, Object instance, Field field, boolean required){
        waitFor(type, new MissingFieldWire(instance, field, required));
    }

    @Override
    public boolean canWait(){
        return canWait;
    }

    public <T extends Annotation> void registerFieldSetup(Class<T> annotationCls, IAnnotatedFieldSetup<T> setupCallback){
        this.annotatedFieldSetupMap.put(annotationCls, setupCallback);
    }

    public <T extends Annotation> void registerMethodSetup(Class<T> annotationCls, IAnnotatedMethodSetup<T> setupCallback){
        this.annotatedMethodSetupMap.put(annotationCls, setupCallback);
    }

    public void registerInstance(Class<?> cls, Object instance) throws MagicInstanceException {
        _registerInstance(cls, new ConstantInstance(instance));
    }

    private void _registerInstance(Class<?> cls, InstanceProvider provider) throws MagicInstanceException {
        instanceMap.put(cls, provider);

        List<MagicWire> waitingWires = waitMap.get(cls);
        if(waitingWires != null){
            for(MagicWire mw : waitingWires){
                Object instance = provider.get();
                mw.accept(instance);
            }

            waitMap.remove(cls);
        }
    }

    @Override
    public <T> T getInstance(Class<T> cls) throws MagicInstanceException {
        InstanceProvider provider = instanceMap.get(cls);
        if(provider == null) return null;

        try {
            return cls.cast(provider.get());
        }
        catch (MagicInstanceException e){
            throw new MagicInstanceException("Failed to get instance for class: [" + cls.getName() + "]!", e);
        }
        catch (ClassCastException e){
            throw new MagicInstanceException("Provider provided incompatible type for class: [" + cls.getName() + "]!", e);
        }
    }

    public void callLifecycleMethods(LifecycleState state) throws MagicInstanceException {
        List<MagicMethod> mms = lifecycleMethods.get(state);

        if(mms == null) return;

        for(MagicMethod mm : mms){
            try {
                mm.invoke();
            }
            catch (MagicInstanceException e){
                throw new MagicInstanceException("Failed to invoke " + state + " method: " + mm, e);
            }
        }
    }
















    private void tryMagicFields(Class<?> cls, Object instance) throws MagicInstanceException {
        Field[] fields = cls.getDeclaredFields();
        for(Field field : fields){
            tryMagicField(instance, field);
        }
    }

    private void tryMagicField(Object instance, Field field) throws MagicInstanceException {
        Annotation[] annotations = field.getDeclaredAnnotations();

        for(Annotation annotation : annotations){
            Class<?> aType = annotation.annotationType();

            IAnnotatedFieldSetup<?> callback = annotatedFieldSetupMap.get(aType);
            if(callback != null){
                callback._setup(annotation, instance, field, this, plugzConfig);
            }
        }
    }

    @Override
    public void setField(Field field, Object instance, Object value) {
        try {
            boolean access = field.canAccess(instance);

            if(!access) field.setAccessible(true);
            field.set(instance, value);
            if(!access) field.setAccessible(false);
        }
        catch (IllegalAccessException e){
            throw new IllegalStateException("INVALID STATE: Field should have been checked!", e);
        }
    }

    @Override
    public Object getField(Field field, Object instance) {
        try {
            boolean access = field.canAccess(instance);

            if(!access) field.setAccessible(true);
            Object value = field.get(instance);
            if(!access) field.setAccessible(false);

            return value;
        }
        catch (IllegalAccessException e){
            throw new IllegalStateException("INVALID STATE: Field should be checked!", e);
        }
    }













    @Override
    public MagicMethod buildMagicMethod(Object instance, Method method) {
        return new MagicMethodImpl(instance, method, null);
    }

    private void tryMagicMethods(Class<?> cls, Object instance) throws MagicInstanceException {
        Method[] methods = cls.getDeclaredMethods();
        for(Method method : methods){
            tryMagicMethod(instance, method);
        }
    }

    private void tryMagicMethod(Object instance, Method method) throws MagicInstanceException{
        Annotation[] annotations = method.getDeclaredAnnotations();

        annotationsLoop:
        for(Annotation annotation : annotations){
            Class<?> aType = annotation.annotationType();

            IAnnotatedMethodSetup<?> callback = annotatedMethodSetupMap.get(aType);
            if(callback != null){
                callback._setup(annotation, instance, method, this, plugzConfig);
                continue;
            }

            if(aType == Async.class) continue;

            if(aType == Get.class){
                tryGetterMethod(instance, method, Get.class, Get::required, (type, getAnnotation) -> getInstance(type));
                continue;
            }

            if(aType == Set.class){
                trySetterMethod(instance, method, Set.class, Set::persist, (type, setAnnotation, provider) -> this._registerInstance(type, provider));
                continue;
            }

            if(aType == GetConfig.class){
                tryGetterMethod(instance, method, GetConfig.class, anno -> true, this::_getConfigValueAsInstanceFunction);
                continue;
            }

            if(aType == SetConfig.class){
                trySetterMethod(instance, method, SetConfig.class, anno -> false, this::_setConfigAsRegisterFunction);
                continue;
            }

            for(LifecycleState state : LifecycleState.values()){
                Class<? extends Annotation> methodAnnotation = state.methodAnnotation;
                if(aType == methodAnnotation){
                    if(state == LifecycleState.PRE_SETUP){
                        tryInvokeMethod(instance, method, canWait, null);
                    }
                    else {
                        lifecycleMethods.computeIfAbsent(state, s -> new ArrayList<>()).add(new MagicMethodImpl(instance, method, null));
                    }
                    continue annotationsLoop;
                }
            }
        }
    }

    private Object _getConfigValueAsInstanceFunction(Class<?> type, GetConfig getConfigAnnotation) {
        String configKey = getConfigAnnotation.value();
        return plugzConfig.get(configKey);
    }

    private void _setConfigAsRegisterFunction(Class<?> type, SetConfig getConfigAnnotation, InstanceProvider provider) throws MagicInstanceException {
        String key = getConfigAnnotation.value();
        Object instance = provider.get();
        plugzConfig.registerConfiguration(key, instance);
    }

    private <T extends Annotation> void tryGetterMethod(Object instance, Method method, Class<T> annotationCls, Function<T, Boolean> requiredFunction, InstanceFunction<T> instanceFunction) throws MagicInstanceException{
        Class<?> getInstanceType = null;
        int _getInstancePos = 0;

        Object[] params = new Object[method.getParameterCount()];
        MissingParamsMethod mpm = new MissingParamsMethod(instance, method, params);

        Parameter[] parameters = method.getParameters();
        for(int i=0;i<parameters.length;i++){
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();
            if(!parameter.isAnnotationPresent(Get.class)){
                if(getInstanceType != null) throw new MagicInstanceException("Magic getter method [" + method + "] cannot have more than 1 non - magic parameter!");
                getInstanceType = parameterType;
                _getInstancePos = i;
            }
            else {
                Get paramGetAnnotation = parameter.getAnnotation(Get.class);
                final boolean required = paramGetAnnotation.required();
                final int pos = i;
                Object paramInstance = getInstance(parameterType);

                if(paramInstance != null){
                    mpm.putParam(pos, paramInstance);
                }
                else {
                    if(canWait) {
                        waitFor(parameterType, new MagicWire() {
                            @Override
                            public String name() {
                                return getMethodString(method);
                            }

                            @Override
                            public boolean required() {
                                return required;
                            }

                            @Override
                            public Class<?> type() {
                                return parameterType;
                            }

                            @Override
                            public void accept(Object o) throws MagicInstanceException {
                                mpm.putParam(pos, o);
                            }
                        });
                    }
                    else {
                        System.out.println("TODO 1");
                    }
                }
            }
        }

        if(getInstanceType == null) throw new MagicInstanceException("Magic getter method [" + method + "] must have 1 non - magic parameter!");

        T getAnnotation = method.getAnnotation(annotationCls);
        Object getInstance = instanceFunction.apply(getInstanceType, getAnnotation);

        if(getInstance != null){
            mpm.putParam(_getInstancePos, getInstance);
        }
        else {
            final boolean required = requiredFunction.apply(getAnnotation);
            final int getInstancePos = _getInstancePos;
            final Class<?> finalGetInstanceType = getInstanceType;

            if(canWait) {
                waitFor(getInstanceType, new MagicWire() {
                    @Override
                    public String name() {
                        return getMethodString(method);
                    }

                    @Override
                    public boolean required() {
                        return required;
                    }

                    @Override
                    public Class<?> type() {
                        return finalGetInstanceType;
                    }

                    @Override
                    public void accept(Object o) throws MagicInstanceException {
                        mpm.putParam(getInstancePos, o);
                    }
                });
            }
            else {
                System.out.println("TODO 2");
            }
        }
    }

    private <T extends Annotation> void trySetterMethod(Object instance, Method method, Class<T> annotationCls, Function<T, Boolean> persistFunction, RegisterInstanceFunction<T> registerFunction) throws MagicInstanceException{
        T setAnnotation = method.getAnnotation(annotationCls);
        boolean persist = persistFunction.apply(setAnnotation);
        Class<?> providedType = method.getReturnType();

        Object[] params = new Object[method.getParameterCount()];
        MethodInstanceProvider mip = new MethodInstanceProvider(instance, method, params, persist);

        Parameter[] parameters = method.getParameters();
        for(int i=0;i<parameters.length;i++){
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();
            if(!parameter.isAnnotationPresent(Get.class)){
                throw new MagicInstanceException("Magic setter method [" + method + "] cannot have non - magic parameters!");
            }
            else {
                Get paramGetAnnotation = parameter.getAnnotation(Get.class);
                final boolean required = paramGetAnnotation.required();
                final int pos = i;
                Object paramInstance = getInstance(parameterType);

                if(paramInstance != null){
                    mip.putParam(pos, paramInstance);
                }
                else {
                    if(canWait) {
                        waitFor(parameterType, new MagicWire() {
                            @Override
                            public String name() {
                                return getMethodString(method);
                            }

                            @Override
                            public boolean required() {
                                return required;
                            }

                            @Override
                            public Class<?> type() {
                                return parameterType;
                            }

                            @Override
                            public void accept(Object o) throws MagicInstanceException {
                                mip.putParam(pos, o);
                                if (mip.missingParams == 0 && !mip.registered) {
                                    mip.registered = true;
                                    registerFunction.register(providedType, setAnnotation, mip);
                                }
                            }
                        });
                    }
                    else {
                        if(required) throw new MagicInstanceException("Could not get nor wait for parameter of type: [" + parameterType + "]!");
                        Object defaultValue = ValueUtils.getDefaultValue(parameterType);
                        mip.putParam(pos, defaultValue);
                        if (mip.missingParams == 0 && !mip.registered) {
                            mip.registered = true;
                            registerFunction.register(providedType, setAnnotation, mip);
                        }
                    }
                }
            }
        }

        if(mip.missingParams == 0 && !mip.registered) {
            mip.registered = true;
            registerFunction.register(providedType, setAnnotation, mip);
        }
    }

    private Object tryInvokeMethod(Object instance, Method method, boolean canWait, Consumer<Object> waitedResponse, Object... parameters) throws MagicInstanceException {
        Object[] params = new Object[method.getParameterCount()];
        AtomicInteger missingParams = new AtomicInteger(params.length);

        int o = 0;
        Parameter[] mParameters = method.getParameters();
        for(int i=0;i<mParameters.length;i++){
            Parameter parameter = mParameters[i];
            Class<?> parameterType = parameter.getType();
            if(!parameter.isAnnotationPresent(Get.class)){
                if(o >= parameters.length) throw new MagicInstanceException("Missing non-magic parameter of type: [" + parameterType + "]!");
                params[i] = parameters[o++];
                missingParams.decrementAndGet();
            }
            else {
                Get paramGetAnnotation = parameter.getAnnotation(Get.class);
                final boolean required = paramGetAnnotation.required();
                final int pos = i;
                Object paramInstance = getInstance(parameterType);

                if(paramInstance != null){
                    params[i] = paramInstance;
                    missingParams.decrementAndGet();
                }
                else {
                    if(canWait) {
                        waitFor(parameterType, new MagicWire() {
                            @Override
                            public String name() {
                                return getMethodString(method);
                            }

                            @Override
                            public boolean required() {
                                return required;
                            }

                            @Override
                            public Class<?> type() {
                                return parameterType;
                            }

                            @Override
                            public void accept(Object o) throws MagicInstanceException {
                                params[pos] = o;
                                missingParams.decrementAndGet();

                                if (missingParams.get() == 0) {
                                    Object result = invokeMethod(instance, method, params);
                                    if(waitedResponse != null) waitedResponse.accept(result);
                                }
                            }
                        });
                    }
                    else {
                        if(required) throw new MagicInstanceException("Could not get nor wait for parameter of type: [" + parameterType + "]!");
                        Object defaultValue = ValueUtils.getDefaultValue(parameterType);
                        params[pos] = defaultValue;
                        missingParams.decrementAndGet();
                    }
                }
            }
        }

        if(missingParams.get() == 0) {
            return invokeMethod(instance, method, params);
        }

        return null;
    }

    private Object invokeMethod(Object instance, Method method, Object... params) throws MagicInstanceException {
        Async async = method.getAnnotation(Async.class);
        int mods = method.getModifiers();

        if(async == null) {
            if (Modifier.isStatic(mods)) {
                return doInvokeMethod(null, method, params);
            }
            else {
                return doInvokeMethod(instance, method, params);
            }
        }
        else {
            CompletableFuture<Object> future = new CompletableFuture<>();

            asyncManager.runAsyncTask(() -> {
                try {
                    Object value;
                    if (Modifier.isStatic(mods)) {
                        value = doInvokeMethod(null, method, params);
                    } else {
                        value = doInvokeMethod(instance, method, params);
                    }

                    future.complete(value);
                }
                catch (MagicInstanceException e){
                    throw new MagicRuntimeException(e);
                }
            }, async.delay());

            return future;
        }
    }

    private Object doInvokeMethod(Object instance, Method method, Object... params) throws MagicInstanceException {
        try {
            boolean canAccess = method.canAccess(instance);

            if(!canAccess) method.setAccessible(true);
            Object ret = method.invoke(instance, params);
            if(!canAccess) method.setAccessible(false);

            return ret;
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException("INVALID STATE: Method [" + getMethodString(method) + "] should not be here when it is private!");
        }
        catch (InvocationTargetException e) {
            throw new MagicInstanceException("Internal exception in magic - method: [" + getMethodString(method) + "]!", e);
        }
    }














    private void createInstanceFromConstructor(Class<?> cls, SetupFunction setupFunction, Object... params) throws MagicInstanceException {
        Constructor<?> preferredConstructor = null;
        int conArgCount = -1;

        Constructor<?>[] constructors = cls.getDeclaredConstructors();
        nextConstructor:
        for(Constructor<?> constructor : constructors){
            Parameter[] parameters = constructor.getParameters();

            int c = 0;
            for(Parameter parameter : parameters){
                Get getAnnotation = parameter.getAnnotation(Get.class);

                if(getAnnotation == null){
                    if(params == null || c >= params.length) continue nextConstructor;
                    Object param = params[c];
                    if(param != null) {
                        if(!parameter.getType().isAssignableFrom(param.getClass())) continue nextConstructor;
                    }
                }
            }

            if(preferredConstructor == null || conArgCount < parameters.length){
                preferredConstructor = constructor;
                conArgCount = parameters.length;
            }
        }

        if(preferredConstructor == null) throw new MagicInstanceException("No suitable constructor found for cls: [" + cls.getName() + "]!");

        tryInvokeConstructor(cls, preferredConstructor, canWait, setupFunction, params);
    }

    private void tryInvokeConstructor(Class<?> cls, Constructor<?> con, boolean canWait, SetupFunction setupFunction, Object... params) throws MagicInstanceException {
        Object[] collectedParams = new Object[con.getParameterCount()];
        MissingParamsConstructor mpc = new MissingParamsConstructor(cls, con, collectedParams, setupFunction);

        int i = 0, c = 0;
        for (Parameter parameter : con.getParameters()) {
            Class<?> parameterType = parameter.getType();
            Get getAnnotation = parameter.getAnnotation(Get.class);

            if (getAnnotation == null) {
                if(params == null || c >= params.length) throw new MagicInstanceException("Invalid number of non - magic parameters!");
                collectedParams[i] = params[c++];
            }
            else {
                Object instance = getInstance(parameterType);

                if(instance == null){
                    if(canWait){
                        final int pos = i;
                        final boolean required = getAnnotation.required();
                        waitFor(parameterType, new MagicWire() {
                            @Override
                            public String name() {
                                return getConstructorString(con);
                            }

                            @Override
                            public boolean required() {
                                return required;
                            }

                            @Override
                            public Class<?> type() {
                                return parameterType;
                            }

                            @Override
                            public void accept(Object o) throws MagicInstanceException {
                                mpc.putParam(pos, o);
                            }
                        });

                    }
                    else {
                        if(getAnnotation.required()) throw new MagicInstanceException("Could not get nor wait for parameter of type: [" + parameterType + "]!");
                        instance = ValueUtils.getDefaultValue(parameterType);
                    }
                }

                collectedParams[i] = instance;
                mpc.missingParams--;
            }

            i++;
        }

        if(mpc.missingParams == 0) {
            mpc.invoke();
        }
    }

    private Object invokeConstructor(Constructor<?> con, Object... args) throws MagicInstanceException {
        try {
            return con.newInstance(args);
        }
        catch (InstantiationException e) {
            throw new IllegalStateException("INVALID STATE: Constructor should not be here when class is abstract!");
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException("INVALID STATE: Constructor should not be here when it is private!");
        }
        catch (InvocationTargetException e) {
            throw new MagicInstanceException("Internal exception in magic - constructor!", e);
        }
    }

}
