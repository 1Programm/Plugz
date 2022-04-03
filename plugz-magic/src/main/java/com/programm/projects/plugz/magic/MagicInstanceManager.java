package com.programm.projects.plugz.magic;

import com.programm.projects.plugz.magic.api.*;
import com.programm.projects.plugz.magic.api.Set;
import com.programm.projects.plugz.magic.api.db.IRepo;
import com.programm.projects.plugz.magic.api.debug.Debug;
import com.programm.projects.plugz.magic.api.debug.IValue;
import com.programm.projects.plugz.magic.api.schedules.Scheduled;
import com.programm.projects.plugz.magic.api.schedules.ScheduledMethodConfig;
import com.programm.projects.plugz.magic.api.web.*;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
class MagicInstanceManager implements IInstanceManager {

    private static String getMethodString(Method method){
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }

    private static String getConstructorString(Constructor<?> con){
        return con.toString();
    }

    @RequiredArgsConstructor
    private class MissingMagicConstructor {
        final Constructor<?> con;
        final Object[] argsArray;
        @Setter
        int numEmptyArgs;

        public void putArg(int pos, Object arg){
            argsArray[pos] = arg;
            numEmptyArgs--;
        }

        public Object tryConstruct() throws MagicInstanceException {
            if(numEmptyArgs > 0) return null;

            if(argsArray.length == 0) {
                return invokeConstructor(con);
            }
            else {
                return invokeConstructor(con, argsArray);
            }
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

    private class ScheduledMagicMethod extends ScheduledMethodConfig {
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

    class WebRequestMethodConfigImpl extends WebRequestMethodConfig {
        private final MagicMethod method;

        public WebRequestMethodConfigImpl(String path, RequestType type, String contentType, List<RequestParam> requestParamAnnotations, int requestBodyAnnotationPos, Class<?> requestBodyType, MagicMethod method) {
            super(path, type, contentType, requestParamAnnotations, requestBodyAnnotationPos, requestBodyType);
            this.method = method;
        }

        @Override
        public Object call(Object... arguments) throws MagicInstanceException {
            return method.call(arguments);
        }
    }

    @RequiredArgsConstructor
    static class DebugFieldConfig {
        final Object instance;
        final Field field;
        final Debug debugAnnotation;
    }

    private interface MagicWire {
        String name();
        String requesterName();
        boolean required();

        void accept(Object o) throws MagicInstanceException;
    }

    @RequiredArgsConstructor
    private static abstract class AMagicWire implements MagicWire {
        private final String name;
        private final String requesterName;
        private final boolean required;

        @Override
        public String name() {
            return name;
        }

        @Override
        public String requesterName() {
            return requesterName;
        }

        @Override
        public boolean required() {
            return required;
        }
    }


    @RequiredArgsConstructor
    private static abstract class AbstractProvider {
        final URL fromUrl;
        final boolean persist;
        final boolean takesName;

        public abstract Object get(String name) throws MagicInstanceException;
    }

    private class MethodProviderConfig extends AbstractProvider {
        final Object instance;
        final Method method;

        public MethodProviderConfig(URL fromUrl, boolean persist, boolean takesName, Object instance, Method method) {
            super(fromUrl, persist, takesName);
            this.instance = instance;
            this.method = method;
        }

        @Override
        public Object get(String name) throws MagicInstanceException{
            if (takesName) {
                return tryInvokeMethod(fromUrl, instance, method, null, false, name);
            } else {
                return tryInvokeMethod(fromUrl, instance, method, null, false);
            }
        }
    }

    private class ClassProviderConfig extends AbstractProvider {
        final Class<?> cls;
        final Constructor<?> con;

        public ClassProviderConfig(URL fromUrl, boolean persist, boolean takesName, Class<?> cls, Constructor<?> con) {
            super(fromUrl, persist, takesName);
            this.cls = cls;
            this.con = con;
        }

        @Override
        public Object get(String name) throws MagicInstanceException {
            if(takesName) {
                return tryInvokeConstructor(fromUrl, cls, con, false, name);
            }
            else {
                return tryInvokeConstructor(fromUrl, cls, con, false);
            }
        }
    }



    private final Map<URL, Map<Class<?>, Object>> instanceMap = new HashMap<>();
    private final Map<URL, Map<Class<?>, List<MagicWire>>> waitMap = new HashMap<>();
    private final Map<URL, Map<Class<?>, AbstractProvider>> providerMap = new HashMap<>();

    private final Map<URL, List<MagicMethod>> postSetupMethods = new HashMap<>();
    private final Map<URL, List<MagicMethod>> preShutdownMethods = new HashMap<>();
    private final Map<URL, List<MagicMethod>> onRemoveMethods = new HashMap<>();

    final Map<URL, List<ScheduledMethodConfig>> toScheduleMethods = new HashMap<>();
    final List<WebRequestMethodConfigImpl> webRequestMethodConfigs = new ArrayList<>();
    final List<DebugFieldConfig> debuggerFieldConfigs = new ArrayList<>();

    private final ThreadPoolManager threadPoolManager;


    public void registerSetClass(Class<?> cls) throws MagicInstanceException {
        Set setAnnotation = cls.getAnnotation(Set.class);
        boolean persist = false;

        if(setAnnotation != null){
            persist = setAnnotation.persist();
        }

        registerProviderForType(cls, cls, persist);
    }

    private void registerProviderForType(Class<?> cls, Class<?> type, boolean persist) throws MagicInstanceException {
        URL url = Utils.getUrlFromClass(cls);

        Constructor<?> fittingConstructor = null;
        boolean conTakesName = false;
        Constructor<?>[] cons = cls.getConstructors();

        for(Constructor<?> con : cons){
            if(con.isAnnotationPresent(Set.class)){
                Parameter[] params = con.getParameters();
                boolean hasName = false;

                for(Parameter param : params){
                    if(!param.isAnnotationPresent(Get.class)){
                        if(hasName || param.getType() != String.class){
                            throw new MagicInstanceException("Constructor [" + con + "] annotated with @Set inside class [" + cls.getName() + "] annotated with @set should have no non-magic parameters or 1 non-magic parameter which can be a string.");
                        }

                        hasName = true;
                    }
                }

                conTakesName = hasName;
                fittingConstructor = con;
                break;
            }
        }

        if(fittingConstructor == null) {
            conLoop:
            for (Constructor<?> con : cons) {
                Parameter[] params = con.getParameters();
                boolean hasName = false;

                for (Parameter param : params) {
                    if (!param.isAnnotationPresent(Get.class)) {
                        if (hasName || param.getType() != String.class) {
                            continue conLoop;
                        }

                        hasName = true;
                    }
                }

                fittingConstructor = con;
                conTakesName = hasName;
                break;
            }
        }

        if(fittingConstructor == null){
            throw new MagicInstanceException("No fitting constructor with 0 or 1 non-magic attribute found.");
        }

        registerProvider(url, type, new ClassProviderConfig(url, persist, conTakesName, cls, fittingConstructor));
    }

    private void registerProvider(URL fromUrl, Class<?> provideType, AbstractProvider provider) throws MagicInstanceException{
        providerMap.computeIfAbsent(fromUrl, url -> new HashMap<>()).put(provideType, provider);

        Map<URL, List<MagicWire>> waitingWires = new HashMap<>();

        for(URL url : waitMap.keySet()){
            Map<Class<?>, List<MagicWire>> waitClasses = waitMap.get(url);
            List<MagicWire> mws = waitClasses.remove(provideType);
            if(mws != null){
                waitingWires.put(url, mws);
                if(waitClasses.isEmpty()){
                    waitMap.remove(url);
                }
            }
        }

        if(!waitingWires.isEmpty()) {
            Object res = null;
            for (URL url : waitingWires.keySet()) {
                List<MagicWire> mws = waitingWires.get(url);
                if (mws != null) {
                    for (MagicWire mw : mws) {
                        if(res == null || !provider.persist) {
                            res = provider.get(mw.name());

                            if(res != null && provider.persist){
                                registerInstance(fromUrl, provideType, res);
                            }
                        }

                        mw.accept(res);
                    }
                }
            }
        }
    }

    @Override
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
            Map<URL, Map<String, List<Class<?>>>> requesterMap = new HashMap<>();

            for(URL url : waitMap.keySet()){
                Map<Class<?>, List<MagicWire>> waits = waitMap.get(url);
                if(waits != null) {
                    for (Class<?> cls : waits.keySet()) {
                        List<MagicWire> mws = waits.get(cls);

                        for(int i=0;i<mws.size();i++){
                            MagicWire wire = mws.get(i);

                            if(!wire.required()){
                                wire.accept(null);
                                mws.remove(i);
                                i--;
                            }
                        }

                        if(mws.isEmpty()) continue;

                        List<String> requestNames = mws.stream().map(MagicWire::requesterName).collect(Collectors.toList());

                        for(String name : requestNames){
                            requesterMap.computeIfAbsent(url, u -> new HashMap<>()).computeIfAbsent(name, n -> new ArrayList<>()).add(cls);
                        }
                    }
                }
            }

            //if(requesterMap.size() == 0) return;
            if(requesterMap.size() == 1){
                for(URL url : requesterMap.keySet()){
                    String waitError = getWaitErrorMessage(requesterMap, url);

                    throw new MagicInstanceException("Waiting for:\n[\n" + waitError + "\n]");
                }
            }
            else if(requesterMap.size() > 1){
                StringBuilder sb1 = new StringBuilder();

                for(URL url : requesterMap.keySet()){
                    if(sb1.length() != 0){
                        sb1.append("\n");
                    }

                    sb1.append("\n[").append(url).append("]:\n");

                    String waitError = getWaitErrorMessage(requesterMap, url);

                    sb1.append("Waiting for:\n[\n").append(waitError).append("\n]");
                }

                throw new MagicInstanceException("\n" + sb1);
            }
        }
    }

    private String getWaitErrorMessage(Map<URL, Map<String, List<Class<?>>>> requesterMap, URL url){
        StringBuilder sb = new StringBuilder();

        Map<String, List<Class<?>>> requesterNameMap = requesterMap.get(url);

        for(String name : requesterNameMap.keySet()){
            List<Class<?>> classes = requesterNameMap.get(name);

            if(sb.length() != 0){
                sb.append(",\n");
            }
            sb.append("   ").append(name).append(" --> ").append(classes);
        }

        return sb.toString();
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

    private void tryMagicFields(URL fromUrl, Class<?> cls, Object instance) throws MagicInstanceException{
        Field[] fields = cls.getDeclaredFields();

        for(Field field : fields){
            if(field.isAnnotationPresent(Get.class)){
                Get getAnnotation = field.getAnnotation(Get.class);
                final String name = getAnnotation.value();
                Class<?> fieldType = field.getType();

                Object val = getInstanceFromCls(fieldType, name);
                if(val != null){
                    putField(instance, field, val);
                    continue;
                }

                MagicWire mw = new AMagicWire(name, cls.toString(), getAnnotation.required()){
                    @Override
                    public void accept(Object o) {
                        putField(instance, field, o);
                    }
                };
                waitMap.computeIfAbsent(fromUrl, url -> new HashMap<>()).computeIfAbsent(fieldType, pt -> new ArrayList<>()).add(mw);
            }
            else if(field.isAnnotationPresent(Debug.class)){
                Debug debugAnnotation = field.getAnnotation(Debug.class);
                debuggerFieldConfigs.add(new DebugFieldConfig(instance, field, debugAnnotation));
            }
        }
    }

    private void tryMagicMethods(URL fromUrl, Class<?> cls, Object instance) throws MagicInstanceException{
        Method[] methods = cls.getDeclaredMethods();

        //WEB
        String parentPath = "";
        if(cls.isAnnotationPresent(RequestMapping.class)){
            parentPath = cls.getAnnotation(RequestMapping.class).value();
        }

        for(Method method : methods){
            final Async asyncAnnotation = method.getAnnotation(Async.class);

            if(method.isAnnotationPresent(Get.class)){
                if(method.getParameterCount() != 1) throw new MagicInstanceException("Method annotated with @Get should be treated like a setter and have only one argument!");

                Get getAnnotation = method.getAnnotation(Get.class);
                final String name = getAnnotation.value();
                Class<?> valType = method.getParameterTypes()[0];

                Object val = getInstanceFromCls(valType, name);
                if(val != null){
                    invokeMethod(instance, method, asyncAnnotation, val);
                    continue;
                }

                MagicWire mw = new AMagicWire(name, cls.toString(), getAnnotation.required()){
                    @Override
                    public void accept(Object o) throws MagicInstanceException {
                        invokeMethod(instance, method, asyncAnnotation, o);
                    }
                };
                waitMap.computeIfAbsent(fromUrl, url -> new HashMap<>()).computeIfAbsent(valType, pt -> new ArrayList<>()).add(mw);
            }
            else if(method.isAnnotationPresent(Set.class)){
                int countNotMagic = 0;
                Parameter[] params = method.getParameters();
                for(int i=0;i<method.getParameterCount();i++){
                    if(!params[i].isAnnotationPresent(Get.class)){
                        countNotMagic++;
                    }
                }

                if(countNotMagic > 1){
                    throw new MagicInstanceException("Method annotated with @Get should can either receive no non-magic parameters or at max 1 as the name of the @Get variable which started this call.");
                }

                Set setAnnotation = method.getAnnotation(Set.class);
                boolean persist = setAnnotation.persist();
                Class<?> provideType = method.getReturnType();

                registerProvider(fromUrl, provideType, new MethodProviderConfig(fromUrl, persist, countNotMagic == 1, instance, method));
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

                if(method.isAnnotationPresent(GetRequest.class)){
                    GetRequest request = method.getAnnotation(GetRequest.class);
                    String fullPath = parentPath + request.value();
                    String contentType = request.contentType();

                    List<RequestParam> requestParamAnnotations = new ArrayList<>();
                    int requestBodyAnnotationPos = -1;
                    Class<?> requestBodyType = null;
                    Annotation[][] paramAnnotations = method.getParameterAnnotations();
                    Class<?>[] paramTypes = method.getParameterTypes();
                    for(int i=0;i<method.getParameterCount();i++){
                        Annotation[] annotations = paramAnnotations[i];
                        for(Annotation a : annotations){
                            if(a instanceof RequestParam){
                                Class<?> paramType = paramTypes[i];
                                if(paramType != String.class) throw new MagicInstanceException("Request parameters can only be of type string!");
                                requestParamAnnotations.add((RequestParam) a);
                                break;
                            }
                            else if(a instanceof RequestBody){
                                requestBodyAnnotationPos = i;
                                requestBodyType = paramTypes[i];
                                break;
                            }
                        }
                    }


                    MagicMethod mm = new MagicMethod(instance, method, fromUrl, asyncAnnotation);
                    webRequestMethodConfigs.add(new WebRequestMethodConfigImpl(fullPath, RequestType.GET, contentType, requestParamAnnotations, requestBodyAnnotationPos, requestBodyType, mm));
                }

                if(method.isAnnotationPresent(PostRequest.class)){
                    PostRequest request = method.getAnnotation(PostRequest.class);
                    String fullPath = parentPath + request.value();
                    String contentType = request.contentType();

                    List<RequestParam> requestParamAnnotations = new ArrayList<>();
                    int requestBodyAnnotationPos = -1;
                    Class<?> requestBodyType = null;
                    Annotation[][] paramAnnotations = method.getParameterAnnotations();
                    Class<?>[] paramTypes = method.getParameterTypes();
                    for(int i=0;i<method.getParameterCount();i++){
                        Annotation[] annotations = paramAnnotations[i];
                        for(Annotation a : annotations){
                            if(a instanceof RequestParam){
                                Class<?> paramType = paramTypes[i];
                                if(paramType != String.class) throw new MagicInstanceException("Request parameters can only be of type string!");
                                requestParamAnnotations.add((RequestParam) a);
                                break;
                            }
                            else if(a instanceof RequestBody){
                                requestBodyAnnotationPos = i;
                                requestBodyType = paramTypes[i];
                                break;
                            }
                        }
                    }

                    MagicMethod mm = new MagicMethod(instance, method, fromUrl, asyncAnnotation);
                    webRequestMethodConfigs.add(new WebRequestMethodConfigImpl(fullPath, RequestType.POST, contentType, requestParamAnnotations, requestBodyAnnotationPos, requestBodyType, mm));
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
        List<Get> getAnnotations = new ArrayList<>();

        for(Constructor<?> con : cls.getConstructors()){
            Annotation[][] annotations = con.getParameterAnnotations();
            boolean correct = true;

            for(int i=0;i<con.getParameterCount();i++){
                boolean found = false;
                for(int o=0;o<annotations[i].length;o++){
                    Annotation an = annotations[i][o];
                    if(an.annotationType() == Get.class){
                        found = true;
                        getAnnotations.add(((Get) an));
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

        if(true) {
            return tryInvokeConstructor(fromUrl, cls, magicConstructor, false);
        }

        int paramCount = magicConstructor.getParameterCount();
        Class<?>[] paramTypes = magicConstructor.getParameterTypes();
        Object[] instances = new Object[paramCount];
        int missingInstances = paramCount;

        MissingMagicConstructor mmc = new MissingMagicConstructor(magicConstructor, instances);

        for(int i=0;i<paramCount;i++){
            Class<?> paramType = paramTypes[i];
            final Get getAnnotation = getAnnotations.get(i);

            Object instance = getInstanceFromCls(paramType, getAnnotation.value());
            if(instance != null){
                instances[i] = instance;
                missingInstances--;
                continue;
            }

            final int pos = i;

            MagicWire mw = new AMagicWire(getAnnotation.value(), cls.toString(), getAnnotation.required()){
                @Override
                public void accept(Object o) throws MagicInstanceException {
                    mmc.putArg(pos, o);
                    Object oInstance = mmc.tryConstruct();

                    if (oInstance != null) {
                        tryMagicFields(fromUrl, cls, oInstance);
                        tryMagicMethods(fromUrl, cls, oInstance);
                        registerInstance(fromUrl, cls, oInstance);
                    }
                }
            };
            waitMap.computeIfAbsent(fromUrl, url -> new HashMap<>()).computeIfAbsent(paramType, pt -> new ArrayList<>()).add(mw);
        }

        if(missingInstances == 0){
            if(instances.length == 0){
                return invokeConstructor(magicConstructor);
            }
            else {
                return invokeConstructor(magicConstructor, instances);
            }
        }

        mmc.setNumEmptyArgs(missingInstances);


        return null;
    }

    public void registerInstance(URL fromUrl, Class<?> cls, Object instance) throws MagicInstanceException {
        //Blacklist
        if(cls == IRepo.class) return;

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

    private Object getInstanceFromCls(Class<?> cls, String name) throws MagicInstanceException {
        Object ret;
        if(name.isEmpty()){
            ret = getInstanceFromClsFromInstances(cls);
            if(ret == null) getInstanceFromClsFromProviders(cls, name);
        }
        else {
            ret = getInstanceFromClsFromProviders(cls, name);
            if(ret == null) getInstanceFromClsFromInstances(cls);
        }

        return ret;
    }

    private Object getInstanceFromClsFromInstances(Class<?> cls){
        for(URL url : instanceMap.keySet()){
            Map<Class<?>, Object> instances = instanceMap.get(url);
            Object instance = instances.get(cls);

            if(instance != null) {
                return instance;
            }
        }

        return null;
    }

    private Object getInstanceFromClsFromProviders(Class<?> cls, String name) throws MagicInstanceException {
        for(URL url : providerMap.keySet()){
            Map<Class<?>, AbstractProvider> providerConfigMap = providerMap.get(url);
            AbstractProvider provider = providerConfigMap.get(cls);

            if(provider != null){
                Object res = provider.get(name);

                if(res != null && provider.persist){
                    registerInstance(provider.fromUrl, cls, res);
                }

                return res;
            }
        }

        return null;
    }

    @Override
    public <T> T getInstance(Class<T> cls) throws MagicInstanceException {
        Object obj = getInstanceFromCls(cls, "");

        if(obj == null){
            throw new MagicInstanceException("No instance for class [" + cls.getName() + "] found!");
        }

        return cls.cast(obj);
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

    private Object tryInvokeConstructor(URL fromUrl, Class<?> cls, Constructor<?> con, boolean acceptsWait, Object... arguments) throws MagicInstanceException {
        int paramCount = con.getParameterCount();
        Class<?>[] paramTypes = con.getParameterTypes();
        Object[] args = new Object[paramCount];
        int missingInstances = paramCount;

        int argumentIndex = 0;

        MissingMagicConstructor mmc = new MissingMagicConstructor(con, args);

        Annotation[][] parameterAnnotations = con.getParameterAnnotations();
        for(int i=0;i<paramCount;i++){
            Class<?> paramType = paramTypes[i];
            Annotation[] paramAnnotations = parameterAnnotations[i];

            Get getAnnotation = null;
            for(Annotation annotation : paramAnnotations){
                if(annotation instanceof Get){
                    getAnnotation = (Get)annotation;
                    break;
                }
            }

            if(getAnnotation == null){
                if(argumentIndex >= arguments.length){
                    throw new MagicInstanceException("Not enough non - magic arguments inside constructor [" + getConstructorString(con) + "].");
                }

                args[i] = arguments[argumentIndex++];
                missingInstances--;
            }
            else {
                final String getName = getAnnotation.value();

                Object val = getInstanceFromCls(paramType, getName);
                if (val != null) {
                    args[i] = val;
                    missingInstances--;
                    continue;
                }

                final int pos = i;

                MagicWire mw = new AMagicWire(getName, cls.toString(), getAnnotation.required()){
                    @Override
                    public void accept(Object o) throws MagicInstanceException {
                        mmc.putArg(pos, o);
                        mmc.tryConstruct();
                    }
                };
                if (!acceptsWait) throw new MagicInstanceException("Constructor [" + getConstructorString(con) + "] expects to get all values and cannot wait for them! - Could not find value for class: [" + paramType.getName() + "]");
                waitMap.computeIfAbsent(fromUrl, url -> new HashMap<>()).computeIfAbsent(paramType, pt -> new ArrayList<>()).add(mw);
            }
        }

        Object ret = null;
        if(missingInstances == 0){
            if(args.length == 0){
                ret = invokeConstructor(con);
            }
            else {
                ret = invokeConstructor(con, args);
            }
        }

        mmc.setNumEmptyArgs(missingInstances);
        return ret;
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

            Get getAnnotation = null;
            for(Annotation annotation : paramAnnotations){
                if(annotation instanceof Get){
                    getAnnotation = (Get)annotation;
                    break;
                }
            }

            if(getAnnotation == null){
                if(argumentIndex >= arguments.length){
                    throw new MagicInstanceException("Not enough non - magic arguments inside method [" + getMethodString(method) + "].");
                }

                args[i] = arguments[argumentIndex++];
                missingInstances--;
            }
            else {
                final String getName = getAnnotation.value();

                Object val = getInstanceFromCls(paramType, getName);
                if (val != null) {
                    args[i] = val;
                    missingInstances--;
                    continue;
                }

                final int pos = i;

                MagicWire mw = new AMagicWire(getName, method.toString(), getAnnotation.required()){
                    @Override
                    public void accept(Object o) throws MagicInstanceException {
                        mmm.putArg(pos, o);
                        mmm.tryInvoke();
                    }
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
