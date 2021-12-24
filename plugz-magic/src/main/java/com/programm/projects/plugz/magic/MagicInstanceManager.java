package com.programm.projects.plugz.magic;

import com.programm.projects.plugz.magic.api.*;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

@RequiredArgsConstructor
class MagicInstanceManager {

    private static String getMethodString(Method method){
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }

    @RequiredArgsConstructor
    private static class MissingMagicConstructor {
        final Constructor<?> con;
        final Object[] argsArray;
        @Setter
        int numEmptyArgs;

        public void putArg(int pos, Object arg){
            argsArray[pos] = arg;
            numEmptyArgs--;
        }

        public Object tryConstruct() throws InstantiationException, IllegalAccessException, InvocationTargetException{
            if(numEmptyArgs > 0) return null;

            return con.newInstance(argsArray);
        }
    }

    @RequiredArgsConstructor
    private class MissingMagicMethod {
        final Object instance;
        final Method method;
        final Async async;
        final Object[] argsArray;

        @Setter
        int numEmptyArgs;

        public void putArg(int pos, Object arg){
            argsArray[pos] = arg;
            numEmptyArgs--;
        }

        public void tryInvoke() throws MagicInstanceException {
            if(numEmptyArgs > 0) return;

            invokeMethod(instance, method, async, argsArray);
        }
    }

    @RequiredArgsConstructor
    private class MagicMethod implements IMagicMethod {
        private final Object instance;
        private final Method method;
        private final URL fromUrl;
        private final Async async;

        @Override
        public Object call(Object... arguments) throws MagicInstanceException {
            return tryInvokeMethod(fromUrl, instance, method, async, false, arguments);
        }

        public void run() throws MagicInstanceException {
            tryInvokeMethod(fromUrl, instance, method, async, false);
        }
    }

    private class ScheduledMagicMethod extends SchedulerMethodConfig {
        private final Object instance;
        private final Method method;
        private final URL fromUrl;

        public ScheduledMagicMethod(long startAfter, long repeatAfter, long stopAfter, Object instance, Method method, URL fromUrl, Async async) {
            super(startAfter, repeatAfter, stopAfter, instance.getClass().getName() + "#" + method.getName(), async);
            this.instance = instance;
            this.method = method;
            this.fromUrl = fromUrl;
        }

        @Override
        public void run() throws MagicInstanceException {
            tryInvokeMethod(fromUrl, instance, method, async, false);
        }
    }

    public interface MagicWire {
        void accept(Object o) throws MagicInstanceException;
    }

    private final Map<URL, Map<Class<?>, Object>> instanceMap = new HashMap<>();
    private final Map<URL, Map<Class<?>, List<MagicWire>>> waitMap = new HashMap<>();

    private final Map<URL, List<MagicMethod>> postSetupMethods = new HashMap<>();
    private final Map<URL, List<MagicMethod>> preShutdownMethods = new HashMap<>();
    private final Map<URL, List<MagicMethod>> onRemoveMethods = new HashMap<>();

    final Map<URL, List<SchedulerMethodConfig>> toScheduleMethods = new HashMap<>();

    private final ThreadPoolManager threadPoolManager;

    public <T> T instantiate(Class<T> cls) throws MagicInstanceException{
        URL url = Utils.getUrlFromClass(cls);

        Object instance = instantiateFromConstructor(url, cls);

        if(instance == null){
            return null; // Must wait for other instances
        }

        tryMagicFields(url, cls, instance);
        tryMagicMethods(url, cls, instance);

        registerInstance(url, cls, instance);

        return cls.cast(instance);
    }

    public Map<Class<?>, Object> removeUrl(URL url, boolean notifyInstances) throws MagicInstanceException{
        //Remove instances
        Map<Class<?>, Object> removedInstances = instanceMap.remove(url);

        //Remove from waitMap
        waitMap.remove(url);

        //Remove postSetupMethods
        postSetupMethods.remove(url);

        //Remove preShutdownMethods
        preShutdownMethods.remove(url);

        //Call onRemoveMethods
        if(notifyInstances) {
            List<MagicMethod> mms = onRemoveMethods.get(url);
            if (mms != null) {
                for (MagicMethod mm : mms) {
                    mm.run();
                }
            }
        }

        //Remove onRemoveMethods
        onRemoveMethods.remove(url);

        return removedInstances;
    }

    public void checkWaitMap() throws MagicInstanceException {
        if(!waitMap.isEmpty()) {
            StringBuilder sb = new StringBuilder();

            for(URL url : waitMap.keySet()){
                Map<Class<?>, List<MagicWire>> waits = waitMap.get(url);
                if(waits != null) {
                    for (Class<?> cls : waits.keySet()) {
                        if (sb.length() != 0) {
                            sb.append(",\n");
                        }
                        sb.append("   ").append(cls.getName());
                    }
                }
            }

            throw new MagicInstanceException("Waiting for:\n[\n" + sb + "\n]");
        }
    }

    public void callPostSetup() throws MagicInstanceException{
        callPostSetupForUrls(postSetupMethods.keySet());
    }

    public void callPostSetupForUrls(Collection<URL> urls) throws MagicInstanceException{
        for(URL url : urls) {
            List<MagicMethod> methods = postSetupMethods.get(url);
            if(methods != null) {
                for (MagicMethod mm : methods) {
                    mm.run();
                }
            }
        }
    }

    public void callPreShutdown() throws MagicInstanceException{
        for(URL url : preShutdownMethods.keySet()) {
            List<MagicMethod> methods = preShutdownMethods.get(url);
            if(methods != null) {
                for (MagicMethod mm : methods) {
                    mm.run();
                }
            }
        }
    }

    public IMagicMethod createMagicMethod(Object instance, Method method){
        URL fromUrl = Utils.getUrlFromClass(instance.getClass());
        return new MagicMethod(instance, method, fromUrl, null);
    }

    private void tryMagicFields(URL fromUrl, Class<?> cls, Object instance){
        Field[] fields = cls.getDeclaredFields();

        for(Field field : fields){
            if(field.isAnnotationPresent(Get.class)){
                Class<?> fieldType = field.getType();

                Object val = getInstanceFromCls(fieldType);
                if(val != null){
                    putField(instance, field, val);
                    continue;
                }

                MagicWire mw = o -> putField(instance, field, o);
                waitMap.computeIfAbsent(fromUrl, url -> new HashMap<>()).computeIfAbsent(fieldType, pt -> new ArrayList<>()).add(mw);
            }
        }
    }

    private void tryMagicMethods(URL fromUrl, Class<?> cls, Object instance) throws MagicInstanceException{
        Method[] methods = cls.getDeclaredMethods();

        for(Method method : methods){
            final Async asyncAnnotation = method.getAnnotation(Async.class);

            if(method.isAnnotationPresent(Get.class)){
                if(method.getParameterCount() != 1) throw new MagicInstanceException("Method annotated with @Get should be treated like a setter and have only one argument!");

                Class<?> valType = method.getParameterTypes()[0];

                Object val = getInstanceFromCls(valType);
                if(val != null){
                    invokeMethod(instance, method, asyncAnnotation, val);
                    continue;
                }

                MagicWire mw = o -> invokeMethod(instance, method, asyncAnnotation, o);
                waitMap.computeIfAbsent(fromUrl, url -> new HashMap<>()).computeIfAbsent(valType, pt -> new ArrayList<>()).add(mw);
            }
            else {
                if(method.isAnnotationPresent(PreSetup.class)){
                    tryInvokeMethod(fromUrl, instance, method, asyncAnnotation, true);
                }

                if(method.isAnnotationPresent(PostSetup.class)){
                    MagicMethod mm = new MagicMethod(instance, method, fromUrl, asyncAnnotation);
                    postSetupMethods.computeIfAbsent(fromUrl, url -> new ArrayList<>()).add(mm);
                }

                if(method.isAnnotationPresent(PreShutdown.class)){
                    MagicMethod mm = new MagicMethod(instance, method, fromUrl, asyncAnnotation);
                    preShutdownMethods.computeIfAbsent(fromUrl, url -> new ArrayList<>()).add(mm);
                }

                if(method.isAnnotationPresent(OnRemove.class)){
                    MagicMethod mm = new MagicMethod(instance, method, fromUrl, asyncAnnotation);
                    onRemoveMethods.computeIfAbsent(fromUrl, url -> new ArrayList<>()).add(mm);
                }

                if(method.isAnnotationPresent(Scheduled.class)){
                    Scheduled ann = method.getAnnotation(Scheduled.class);
                    ScheduledMagicMethod scheduledMagicMethod = new ScheduledMagicMethod(ann.startAfter(), ann.repeat(), ann.stopAfter(), instance, method, fromUrl, asyncAnnotation);
                    toScheduleMethods.computeIfAbsent(fromUrl, url -> new ArrayList<>()).add(scheduledMagicMethod);
                }
            }
        }
    }

    private Object instantiateFromConstructor(URL fromUrl, Class<?> cls) throws MagicInstanceException {
        try {
            Constructor<?> con = cls.getConstructor();
            try {
                return con.newInstance();
            }
            catch (InstantiationException e) {
                throw new MagicInstanceException("Class is abstract or an Interface: [" + cls.getName() + "]!", e);
            }
            catch (InvocationTargetException e) {
                throw new MagicInstanceException("Empty constructor threw an Exception!", e);
            }
            catch (IllegalAccessException ignore) {}
        }
        catch (NoSuchMethodException ignore){}

        Constructor<?> magicConstructor = null;

        for(Constructor<?> con : cls.getConstructors()){
            Annotation[][] annotations = con.getParameterAnnotations();
            boolean correct = true;

            for(int i=0;i<con.getParameterCount();i++){
                boolean found = false;
                for(int o=0;o<annotations[i].length;o++){
                    Annotation an = annotations[i][o];
                    if(an.annotationType() == Get.class){
                        found = true;
                        break;
                    }
                }

                if(!found){
                    correct = false;
                    break;
                }
            }

            if(correct) {
                magicConstructor = con;
                break;
            }
        }

        if(magicConstructor == null){
            throw new MagicInstanceException("Could not find appropriate constructor: No public empty - or public magic - Constructor found for class [" + cls.getName() + "]!");
        }


        //Try to get all instances

        int paramCount = magicConstructor.getParameterCount();
        Class<?>[] paramTypes = magicConstructor.getParameterTypes();
        Object[] instances = new Object[paramCount];
        int missingInstances = paramCount;

        MissingMagicConstructor mmc = new MissingMagicConstructor(magicConstructor, instances);

        for(int i=0;i<paramCount;i++){
            Class<?> paramType = paramTypes[i];

            Object instance = getInstanceFromCls(paramType);
            if(instance != null){
                instances[i] = instance;
                missingInstances--;
                continue;
            }

            final int pos = i;
            MagicWire mw = o -> {
                mmc.putArg(pos, o);
                try {
                    Object oInstance = mmc.tryConstruct();

                    if (oInstance != null) {
                        tryMagicFields(fromUrl, cls, oInstance);
                        tryMagicMethods(fromUrl, cls, oInstance);
                        registerInstance(fromUrl, cls, oInstance);
                    }
                }
                catch (InstantiationException e) {
                    throw new MagicInstanceException("Class is abstract or an Interface: [" + cls.getName() + "]!", e);
                }
                catch (IllegalAccessException e) {
                    throw new MagicInstanceException("Constructor suddenly went private ... idk why :D", e);
                }
                catch (InvocationTargetException e) {
                    throw new MagicInstanceException("Underlying constructor threw an exception!", e);
                }
            };

            waitMap.computeIfAbsent(fromUrl, url -> new HashMap<>()).computeIfAbsent(paramType, pt -> new ArrayList<>()).add(mw);
        }

        if(missingInstances == 0){
            return invokeConstructor(magicConstructor, instances);
        }

        mmc.setNumEmptyArgs(missingInstances);


        return null;
    }

    public void registerInstance(URL fromUrl, Class<?> cls, Object instance) throws MagicInstanceException {
        Class<?>[] interfaces = cls.getInterfaces();
        for(Class<?> iCls : interfaces){
            registerInstance(fromUrl, iCls, instance);
        }

        instanceMap.computeIfAbsent(fromUrl, url -> new HashMap<>()).put(cls, instance);

        Map<URL, List<MagicWire>> waitingWires = new HashMap<>();

        for(URL url : waitMap.keySet()){
            List<MagicWire> mws = waitMap.get(url).get(cls);
            if(mws != null){
                waitingWires.put(url, mws);
            }
        }

        for(URL url : waitingWires.keySet()){
            List<MagicWire> mws = waitingWires.get(url);
            for(MagicWire mw : mws) {
                mw.accept(instance);
            }
        }

        for(URL url : waitingWires.keySet()) {
            Map<Class<?>, List<MagicWire>> wireMap = waitMap.get(url);
            if(wireMap != null){
                wireMap.remove(cls);

                if(wireMap.size() == 0){
                    waitMap.remove(url);
                }
            }
            else {
                waitMap.remove(url);
            }
        }
    }

    private Object getInstanceFromCls(Class<?> cls){
        Object instance = null;

        for(URL url : instanceMap.keySet()){
            Map<Class<?>, Object> instances = instanceMap.get(url);
            instance = instances.get(cls);

            if(instance != null) break;
        }

        return instance;
    }

    <T> T getInstance(Class<T> cls) throws MagicInstanceException {
        Object obj = getInstanceFromCls(cls);

        if(obj == null){
            throw new MagicInstanceException("No instance for class [" + cls.getName() + "] found!");
        }

        return cls.cast(obj);
    }

    private Object invokeConstructor(Constructor<?> con, Object[] args) throws MagicInstanceException {
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

    private void putField(Object instance, Field field, Object val) {
        try {
            boolean access = field.canAccess(instance);
            if(!access) field.setAccessible(true);
            field.set(instance, val);
            if(!access) field.setAccessible(false);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException("INVALID STATE: Field should not be private!");
        }
    }

    private Object tryInvokeMethod(URL fromUrl, Object instance, Method method, Async async, boolean acceptsWait, Object... arguments) throws MagicInstanceException {
        int paramCount = method.getParameterCount();
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramCount];
        int missingInstances = paramCount;

        int argumentIndex = 0;

        MissingMagicMethod mmm = new MissingMagicMethod(instance, method, async, args);

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for(int i=0;i<paramCount;i++){
            Class<?> paramType = paramTypes[i];
            Annotation[] paramAnnotations = parameterAnnotations[i];

            boolean isGetNotPresent = true;
            for(Annotation annotation : paramAnnotations){
                if(annotation instanceof Get){
                    isGetNotPresent = false;
                    break;
                }
            }

            if(isGetNotPresent){
                if(argumentIndex >= arguments.length){
                    throw new MagicInstanceException("Not enough non - magic arguments inside method [" + getMethodString(method) + "].");
                }

                args[i] = arguments[argumentIndex++];
                missingInstances--;
            }
            else {
                Object val = getInstanceFromCls(paramType);
                if (val != null) {
                    args[i] = val;
                    missingInstances--;
                    continue;
                }

                final int pos = i;
                MagicWire mw = o -> {
                    mmm.putArg(pos, o);
                    mmm.tryInvoke();
                };

                if (!acceptsWait) throw new MagicInstanceException("Method [" + getMethodString(method) + "] expects to get all values and cannot wait for them! - Could not find value for class: [" + paramType.getName() + "]");
                waitMap.computeIfAbsent(fromUrl, url -> new HashMap<>()).computeIfAbsent(paramType, pt -> new ArrayList<>()).add(mw);
            }
        }

        Object ret = null;
        if(missingInstances == 0){
            if(args.length == 0){
                ret = invokeMethod(instance, method, async);
            }
            else {
                ret = invokeMethod(instance, method, async, args);
            }
        }

        mmm.setNumEmptyArgs(missingInstances);
        return ret;
    }

    private Object invokeMethod(Object instance, Method method, Async async, Object... args) throws MagicInstanceException{
        if(async == null){
            return doInvokeMethod(instance, method, args);
        }
        else {
            threadPoolManager.runAsyncTask(new Runnable(){
                public void run() {
                    try {
                        doInvokeMethod(instance, method, args);
                    } catch (MagicInstanceException e) {
                        throw new MagicRuntimeException(e);
                    }
                }

                @Override
                public String toString() {
                    return method.toString();
                }
            }, async.delay());

            return null;
        }
    }

    private Object doInvokeMethod(Object instance, Method method, Object... args) throws MagicInstanceException {
        try {
            boolean canAccess = method.canAccess(instance);

            if(!canAccess) method.setAccessible(true);

            Object ret = method.invoke(instance, args);

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

}
