package com.programm.projects.plugz.magic;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MagicInstanceManager {

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
    private static class MissingMagicMethod {
        final Method method;
        final Object[] argsArray;
        @Setter
        int numEmptyArgs;

        public void putArg(int pos, Object arg){
            argsArray[pos] = arg;
            numEmptyArgs--;
        }

        public void tryInvoke() throws IllegalAccessException, InvocationTargetException{
            if(numEmptyArgs > 0) return;

            method.invoke(argsArray);
        }
    }

    @RequiredArgsConstructor
    private class MagicMethod {
        private final Object instance;
        private final Method method;

        public void run() throws MagicInstanceException {
            tryInvokeMethod(instance, method);
        }
    }

    private interface MagicWire {

        void accept(Object o) throws MagicInstanceException;

    }

    private final Map<Class<?>, Object> instanceMap = new HashMap<>();
    private final Map<Class<?>, List<MagicWire>> waitMap = new HashMap<>();

    private final List<MagicMethod> postSetupMethods = new ArrayList<>();
    private final List<MagicMethod> preShutdownMethods = new ArrayList<>();

    public void instantiate(Class<?> cls) throws MagicInstanceException{
        Object instance = instantiateFromConstructor(cls);

        if(instance == null){
            return; // Must wait for other instances
        }

        tryMagicFields(cls, instance);
        tryMagicMethods(cls, instance);

        registerInstance(cls, instance);
    }

    public void checkWaitMap() throws MagicInstanceException {
        if(!waitMap.isEmpty()) {
            StringBuilder sb = new StringBuilder();

            for(Class<?> cls : waitMap.keySet()){
                if(sb.length() != 0){
                    sb.append(",\n");
                }
                sb.append("   ").append(cls.getName());
            }

            throw new MagicInstanceException("Waiting for:\n[\n" + sb + "\n]");
        }
    }

    public void callPostSetup() throws MagicInstanceException{
        for(MagicMethod mm : postSetupMethods){
            mm.run();
        }
    }

    public void callPreShutdown() throws MagicInstanceException{
        for(MagicMethod mm : preShutdownMethods){
            mm.run();
        }
    }

    private void tryMagicFields(Class<?> cls, Object instance){
        Field[] fields = cls.getDeclaredFields();

        for(Field field : fields){
            if(field.isAnnotationPresent(Get.class)){
                Class<?> fieldType = field.getType();

                Object val = instanceMap.get(fieldType);
                if(val != null){
                    putField(instance, field, val);
                    continue;
                }

                MagicWire mw = o -> putField(instance, field, o);
                waitMap.computeIfAbsent(fieldType, pt -> new ArrayList<>()).add(mw);
            }
        }
    }

    private void tryMagicMethods(Class<?> cls, Object instance) throws MagicInstanceException{
        Method[] methods = cls.getDeclaredMethods();

        for(Method method : methods){
            if(method.isAnnotationPresent(Get.class)){
                if(method.getParameterCount() != 1) throw new MagicInstanceException("Method annotated with @Get should be treated like a setter and have only one argument!");

                Class<?> valType = method.getParameterTypes()[0];

                Object val = instanceMap.get(valType);
                if(val != null){
                    invokeMethod(instance, method, val);
                    continue;
                }

                MagicWire mw = o -> invokeMethod(instance, method, o);
                waitMap.computeIfAbsent(valType, pt -> new ArrayList<>()).add(mw);
            }

            else if(method.isAnnotationPresent(PreSetup.class)){
                tryInvokeMethod(instance, method);
            }
            else if(method.isAnnotationPresent(PostSetup.class)){
                MagicMethod mm = new MagicMethod(instance, method);
                postSetupMethods.add(mm);
            }
            else if(method.isAnnotationPresent(PreShutdown.class)){
                MagicMethod mm = new MagicMethod(instance, method);
                preShutdownMethods.add(mm);
            }
        }
    }

    private Object instantiateFromConstructor(Class<?> cls) throws MagicInstanceException {
        try {
            Constructor<?> con = cls.getConstructor();
            try {
                return con.newInstance();
            } catch (InstantiationException e) {
                throw new MagicInstanceException("Class is abstract or an Interface: [" + cls.getName() + "]!", e);
            }  catch (InvocationTargetException e) {
                throw new MagicInstanceException("Empty constructor threw an Exception!", e);
            } catch (IllegalAccessException ignore) {}
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

            Object instance = instanceMap.get(paramType);
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
                        registerInstance(cls, oInstance);
                    }
                } catch (InstantiationException e) {
                    throw new MagicInstanceException("Class is abstract or an Interface: [" + cls.getName() + "]!", e);
                } catch (IllegalAccessException e) {
                    throw new MagicInstanceException("Constructor suddenly went private ... idk why :D", e);
                } catch (InvocationTargetException e) {
                    throw new MagicInstanceException("Underlying constructor threw an exception!", e);
                }
            };

            waitMap.computeIfAbsent(paramType, pt -> new ArrayList<>()).add(mw);
        }

        if(missingInstances == 0){
            return invokeConstructor(magicConstructor, instances);
        }

        mmc.setNumEmptyArgs(missingInstances);


        return null;
    }

    private void registerInstance(Class<?> cls, Object instance) throws MagicInstanceException {
        instanceMap.put(cls, instance);

        List<MagicWire> mws = waitMap.get(cls);

        if(mws != null){
            for(MagicWire mw : mws){
                mw.accept(instance);
            }

            waitMap.remove(cls);
        }
    }

    private Object invokeConstructor(Constructor<?> con, Object[] args) throws MagicInstanceException {
        try {
            return con.newInstance(args);
        } catch (InstantiationException e) {
            throw new IllegalStateException("INVALID STATE: Constructor should not be here when class is abstract!");
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("INVALID STATE: Constructor should not be here when it is private!");
        } catch (InvocationTargetException e) {
            throw new MagicInstanceException("Internal exception in magic - constructor!", e);
        }
    }

    private void putField(Object instance, Field field, Object val) {
        try {
            boolean access = field.canAccess(instance);
            if(!access) field.setAccessible(true);
            field.set(instance, val);
            if(!access) field.setAccessible(false);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("INVALID STATE: Field should not be private!");
        }
    }

    private void tryInvokeMethod(Object instance, Method method) throws MagicInstanceException {
        int paramCount = method.getParameterCount();
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramCount];
        int missingInstances = paramCount;

        MissingMagicMethod mmm = new MissingMagicMethod(method, args);

        for(int i=0;i<paramCount;i++){
            Class<?> paramType = paramTypes[i];

            Object val = instanceMap.get(paramType);
            if(val != null){
                args[i] = val;
                missingInstances--;
                continue;
            }

            final int pos = i;
            MagicWire mw = o -> {
                mmm.putArg(pos, o);
                try {
                    mmm.tryInvoke();
                } catch (IllegalAccessException e) {
                    throw new MagicInstanceException("Method suddenly went private ... idk why :D", e);
                } catch (InvocationTargetException e) {
                    throw new MagicInstanceException("Underlying method threw an exception!", e);
                }
            };

            waitMap.computeIfAbsent(paramType, pt -> new ArrayList<>()).add(mw);
        }

        if(missingInstances == 0){
            if(args.length == 0){
                invokeMethod(instance, method);
            }
            else {
                invokeMethod(instance, method, args);
            }
        }

        mmm.setNumEmptyArgs(missingInstances);
    }

    private void invokeMethod(Object instance, Method method, Object... args) throws MagicInstanceException{
        try {
            method.invoke(instance, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("INVALID STATE: Method should not be here when it is private!");
        } catch (InvocationTargetException e) {
            throw new MagicInstanceException("Internal exception in magic - method!", e);
        }
    }

}
