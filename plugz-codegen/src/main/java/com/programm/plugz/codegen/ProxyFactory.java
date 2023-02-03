package com.programm.plugz.codegen;

import com.programm.plugz.codegen.codegenerator.JavaCodeGenerationException;
import com.programm.plugz.codegen.codegenerator.JavaCodeGenerator;
import com.programm.plugz.codegen.codegenerator.Visibility;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class ProxyFactory {

    private static final String PROXY_FIELD_HANDLER = "$handler";
    private static final Map<Class<?>, Class<?>> CASHED_LOG_PROXY_CLASS_MAP = new HashMap<>();
    private static final Map<Class<?>, Class<?>> CASHED_PROXY_CLASS_MAP = new HashMap<>();

    private static boolean doCaching = true;

    public static void doCaching(boolean caching){
        doCaching = caching;
    }




    public static Class<?> createLogProxyClass(Class<?> superClass) throws ProxyClassCreationException {
        Class<?> cls = doCaching ? CASHED_LOG_PROXY_CLASS_MAP.get(superClass) : null;
        if(cls == null) {
            String superClassCanonicalName = superClass.getCanonicalName();
            if(superClassCanonicalName == null) throw new ProxyClassCreationException("Superclass cannot be a local, anonymous or hidden class [" + superClass + "]!");

            String packageName = superClass.getPackageName();
            String className = superClass.getSimpleName();
            try {
                cls = JavaCode.createAndCompileClass(packageName, className, (g, name) -> {
                    if(!packageName.isEmpty()) g.definePackage(packageName);
                    g.defineClass(name);
                    g.defineExtends(superClassCanonicalName);
                    generateLogProxyClass(g, superClass);
                });

                if(doCaching) CASHED_LOG_PROXY_CLASS_MAP.put(superClass, cls);
            }
            catch (JavaCodeGenerationException e){
                throw new ProxyClassCreationException("Failed to create and compile proxy for superclass: " + superClass + "!", e);
            }
        }

        return cls;
    }

    @SuppressWarnings("unchecked")
    public static <T> T createLogProxy(Class<? super T> superClass) throws ProxyClassCreationException {
        Class<?> cls = createLogProxyClass(superClass);

        Constructor<?> con = findEmptyConstructor(cls);
        if(con == null) throw new ProxyClassCreationException("Could not find empty constructor for class [" + superClass.getName() + "]!");

        try {
            return (T) con.newInstance();
        }
        catch (InstantiationException | InvocationTargetException | IllegalAccessException e){
            throw new ProxyClassCreationException("Failed to call constructor!", e);
        }
    }

    public static Class<?> createProxyClass(Class<?> superClass) throws ProxyClassCreationException {
        Class<?> cls = doCaching ? CASHED_PROXY_CLASS_MAP.get(superClass) : null;

        if(cls == null){
            String superClassCanonicalName = superClass.getCanonicalName();
            if(superClassCanonicalName == null) throw new ProxyClassCreationException("Superclass cannot be a local, anonymous or hidden class [" + superClass + "]!");

            String packageName = superClass.getPackageName();
            String className = superClass.getSimpleName();
            try {
                cls = JavaCode.createAndCompileClass(packageName, className, (g, name) -> {
                    if(!packageName.isEmpty()) g.definePackage(packageName);
                    g.defineClass(name);
                    g.defineExtends(superClassCanonicalName);
                    generateProxyClass(g, superClass);
                });

                if(doCaching) CASHED_PROXY_CLASS_MAP.put(superClass, cls);
            }
            catch (JavaCodeGenerationException e){
                throw new ProxyClassCreationException("Failed to create and compile proxy for superclass: " + superClass + "!", e);
            }
        }

        return cls;
    }

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<? super T> superClass, ProxyMethodHandler methodHandler, Object... constructorArgs) throws ProxyClassCreationException {
        Class<?> cls = createProxyClass(superClass);
        Constructor<?> con = findFittingConstructorFromArgs(cls, constructorArgs);
        if(con == null) throw new ProxyClassCreationException("Could not find a fitting constructor for arguments: " + Arrays.toString(constructorArgs) + "!");

        Object proxyInstance = createAndSetupProxyHandler(cls, con, methodHandler, constructorArgs);
        return (T) proxyInstance;
    }

    public static Object[] createMultipleProxies(ProxyMethodHandler methodHandler, Class<?>... superClasses) throws ProxyClassCreationException {
        int len = superClasses.length;

        File[] toCompileSourceCodeFiles = new File[len];
        Class<?>[] proxyClasses = new Class[len];

        int lenToCompile = 0;
        for(int i=0;i<len;i++){
            Class<?> superClass = superClasses[i];
            Class<?> cls = doCaching ? CASHED_PROXY_CLASS_MAP.get(superClass) : null;

            if(cls != null) {
                toCompileSourceCodeFiles[i] = null;
                proxyClasses[i] = cls;
                continue;
            }

            String superClassCanonicalName = superClass.getCanonicalName();
            if(superClassCanonicalName == null) throw new ProxyClassCreationException("Superclass cannot be a local, anonymous or hidden class [" + superClass + "]!");

            String packageName = superClass.getPackageName();
            String className = superClass.getSimpleName();

            try {
                File sourceCodeFile = JavaCode.writeCodeToFile(packageName, className, (g, name) -> {
                    if(!packageName.isEmpty()) g.definePackage(packageName);
                    g.defineClass(name);
                    g.defineExtends(superClassCanonicalName);
                    generateProxyClass(g, superClass);
                });

                toCompileSourceCodeFiles[i] = sourceCodeFile;
            }
            catch (JavaCodeGenerationException e){
                throw new ProxyClassCreationException("Failed to create and compile proxy for superclass: " + superClass + "!", e);
            }

            lenToCompile++;
        }

        URL url = null;
        String[] classNamesToCompile = new String[lenToCompile];

        for(int i=0;i<len;i++){
            File sourceFile = toCompileSourceCodeFiles[i];
            if(sourceFile == null) continue;
            Class<?> superClass = superClasses[i];
            String packageName = superClass.getPackageName();
            File parentDirectory = sourceFile.getParentFile();
            String generatedClassName = sourceFile.getName().split("\\.")[0];
            String fullGeneratedClassName = packageName.isEmpty() ? generatedClassName : packageName + "." + generatedClassName;

            try {
                JavaCode.compileSourceCode(sourceFile, parentDirectory);
            }
            catch (IOException e){
                throw new ProxyClassCreationException("Failed to compile source code of proxy class [" + fullGeneratedClassName + "]!", e);
            }

            if(url == null){
                try {
                    url = parentDirectory.toURI().toURL();
                }
                catch (MalformedURLException e){
                    throw new ProxyClassCreationException("Malformed url from generated source directory!", e);
                }
            }

            classNamesToCompile[i] = fullGeneratedClassName;
        }

        if(lenToCompile > 0){
            try {
                Class<?>[] compiledClasses = JavaCode.loadClassesFromUrl(new URL[]{url}, classNamesToCompile);

                int o = 0;
                for(int i=0;i<len;i++){
                    if(proxyClasses[i] == null){
                        Class<?> superClass = superClasses[i];
                        Class<?> compiledClass = compiledClasses[o++];
                        proxyClasses[i] = compiledClass;
                        if(doCaching) CASHED_PROXY_CLASS_MAP.put(superClass, compiledClass);
                    }
                }
            }
            catch (ClassNotFoundException e){
                throw new ProxyClassCreationException("Failed to load compiled proxy classes!", e);
            }
        }


        Object[] result = new Object[len];
        for(int i=0;i<len;i++){
            Class<?> proxyClass = proxyClasses[i];

            Constructor<?> con;
            try {
                con = proxyClass.getConstructor();
            }
            catch (NoSuchMethodException e){
                throw new ProxyClassCreationException("No empty constructor for proxy class: " + superClasses[i] + "!", e);
            }

            result[i] = createAndSetupProxyHandler(proxyClass, con, methodHandler);
        }

        return result;
    }

    private static Object createAndSetupProxyHandler(Class<?> cls, Constructor<?> constructor, ProxyMethodHandler methodHandler, Object... args) throws ProxyClassCreationException {
        Object proxyInstance;
        try {
            proxyInstance = constructor.newInstance(args);
        }
        catch (InstantiationException | InvocationTargetException | IllegalAccessException e){
            throw new ProxyClassCreationException("Failed to call constructor!", e);
        }

        Field handlerField;
        try {
            handlerField = cls.getField(PROXY_FIELD_HANDLER);
        }
        catch (NoSuchFieldException e){
            throw new IllegalStateException("INVALID STATE: Should have generated a field with name: [" + PROXY_FIELD_HANDLER + "]!", e);
        }

        try {
            handlerField.set(proxyInstance, methodHandler);
        }
        catch (IllegalAccessException e){
            throw new IllegalStateException("INVALID STATE: The generated field [" + PROXY_FIELD_HANDLER + "] should be accessible!", e);
        }

        return proxyInstance;
    }

    private static void generateLogProxyClass(JavaCodeGenerator g, Class<?> superClass) throws JavaCodeGenerationException {
        g.startBlock();

        Method[] methods = superClass.getMethods();
        for(Method method : methods){
            int modifiers = method.getModifiers();
            if(Modifier.isFinal(modifiers) || Modifier.isNative(modifiers) || Modifier.isStatic(modifiers)) continue;

            Visibility visibility = getVisibilityThroughModifiers(modifiers);
            Class<?> returnType = method.getReturnType();
            String methodName = method.getName();

            Class<?>[] methodParameterTypes = method.getParameterTypes();
            StringBuilder argsNamesAsInput = new StringBuilder();
            collectMethodParamInfo(method, argsNamesAsInput, null);

            g.defineMethod(methodName, returnType, methodParameterTypes, visibility);
            g.startBlock();
            g.defineStatement("System.out.println(\"Logging Method [" + methodName + "]\");");

            StringBuilder sb = new StringBuilder();
            if(returnType != Void.TYPE){
                sb.append("return ");
            }
            sb.append("super.").append(methodName).append("(").append(argsNamesAsInput).append(");");
            g.defineStatement(sb.toString());
            g.endBlock();
        }

        g.endBlock();
    }

    private static void generateProxyClass(JavaCodeGenerator g, Class<?> superClass) throws JavaCodeGenerationException{
        g.startBlock();
        g.defineMember(PROXY_FIELD_HANDLER, ProxyMethodHandler.class, Visibility.PUBLIC);

        Set<String> alreadyGeneratedMethods = new HashSet<>();

        Method[] declaredMethods = superClass.getDeclaredMethods();
        for(Method method : declaredMethods){
            alreadyGeneratedMethods.add(method.toGenericString());
            generateHandledMethod(g, superClass, method);
        }

        Method[] methods = superClass.getMethods();
        for(Method method : methods){
            String gmName = method.toGenericString();
            if(alreadyGeneratedMethods.contains(gmName)) continue;
            generateHandledMethod(g, superClass, method);
        }

        g.endBlock();
    }

    private static void generateHandledMethod(JavaCodeGenerator g, Class<?> superClass, Method m) throws JavaCodeGenerationException {
        int modifiers = m.getModifiers();
        if(Modifier.isFinal(modifiers) || Modifier.isNative(modifiers) || Modifier.isStatic(modifiers)) return;

        Visibility visibility = getVisibilityThroughModifiers(modifiers);
        Class<?> returnType = m.getReturnType();
        String methodName = m.getName();

        Class<?>[] methodParameterTypes = m.getParameterTypes();
        StringBuilder argsNamesAsInput = new StringBuilder();
        StringBuilder argsTypesAsInput = new StringBuilder();
        collectMethodParamInfo(m, argsNamesAsInput, argsTypesAsInput);

        g.defineMethod(m.getName(), returnType, methodParameterTypes, visibility);

        Class<?>[] methodExceptionTypes = m.getExceptionTypes();
        for(Class<?> exceptionType : methodExceptionTypes) {
            g.defineThrowsDeclaration(exceptionType);
        }

        g.startBlock();
        g.defineVariable("m", Method.class);
        g.defineTryStatement();
        g.startBlock();
        StringBuilder sb = new StringBuilder();
        sb.append("m = ").append(superClass.getCanonicalName()).append(".class.getMethod(\"").append(methodName).append("\"");
        if(!argsTypesAsInput.isEmpty()) sb.append(", ").append(argsTypesAsInput);
        sb.append(");");
        g.defineStatement(sb.toString());
        g.endBlock();
        g.defineCatchStatement(NoSuchMethodException.class);
        g.startBlock();
        g.defineStatement("throw new " + IllegalStateException.class.getName() + "(\"ILLEGAL STATE: Method should exist!\", e);");
        g.endBlock();
        g.defineIfStatement(PROXY_FIELD_HANDLER + ".canHandle(this, m)");
        g.startBlock();
        g.defineTryStatement();
        g.startBlock();
        sb = new StringBuilder();
        if(returnType != Void.TYPE) {
            sb.append("return (").append(returnType.getName()).append(") ");
        }
        sb.append(PROXY_FIELD_HANDLER).append(".invoke(this, ").append(GeneratedCodeHelper.class.getName()).append(".wrapMethod(").append(superClass.getCanonicalName()).append(".class").append(", ");
        sb.append("m, (args) -> ");

        if(returnType == Void.TYPE) sb.append("{");

        sb.append("super.").append(methodName).append("(");

        for(int i=0;i<methodParameterTypes.length;i++){
            if(i != 0) sb.append(", ");
            String paramTypeName = methodParameterTypes[i].getName();
            sb.append("(").append(paramTypeName).append(") args[").append(i).append("]");
        }
        sb.append(")");

        if(returnType == Void.TYPE) {
            sb.append("; return null; }");
        }
        sb.append(")");

        if(!argsNamesAsInput.isEmpty()) sb.append(", ").append(argsNamesAsInput);
        sb.append(");\n");

        g.defineStatement(sb.toString());
        g.endBlock();
        g.defineCatchStatement(Exception.class);
        g.startBlock();
        g.defineStatement("throw new " + ProxyClassRuntimeException.class.getName() + "(e);");
        g.endBlock();
        g.endBlock();
        g.defineElseStatement();
        g.startBlock();
        sb = new StringBuilder();
        if(returnType != Void.TYPE){
            sb.append("return ");
        }
        sb.append("super.").append(methodName).append("(").append(argsNamesAsInput).append(");");
        g.defineStatement(sb.toString());
        g.endBlock();
        g.endBlock();
    }

    private static Visibility getVisibilityThroughModifiers(int mods){
        Visibility visibility = Visibility.PACKAGE_PRIVATE;
        if(Modifier.isPrivate(mods)) visibility = Visibility.PRIVATE;
        else if(Modifier.isProtected(mods)) visibility = Visibility.PROTECTED;
        else if(Modifier.isPublic(mods)) visibility = Visibility.PUBLIC;
        return visibility;
    }

    private static void collectMethodParamInfo(Method method, StringBuilder argsNamesAsInput, StringBuilder argsTypesAsInput){
        Class<?>[] methodParameterTypes = method.getParameterTypes();
        for(int i=0;i<methodParameterTypes.length;i++){
            Class<?> paramType = methodParameterTypes[i];
            if(i != 0) {
                if(argsNamesAsInput != null) argsNamesAsInput.append(", ");
                if(argsTypesAsInput != null) argsTypesAsInput.append(", ");
            }

            if(argsNamesAsInput != null) argsNamesAsInput.append("v").append(i);
            if(argsTypesAsInput != null) argsTypesAsInput.append(paramType.getName()).append(".class");
        }
    }

    private static Constructor<?> findFittingConstructorFromArgs(Class<?> cls, Object[] args){
        Class<?>[] constructorParamTypes = new Class[args.length];
        for(int i=0;i<args.length;i++){
            Object arg = args[i];
            if(arg != null){
                constructorParamTypes[i] = arg.getClass();
            }
        }

        return findFittingConstructor(cls, constructorParamTypes);
    }

    private static Constructor<?> findFittingConstructor(Class<?> cls, Class<?>[] types){
        Constructor<?>[] constructors = cls.getDeclaredConstructors();

        outerLoop:
        for(Constructor<?> constructor : constructors){
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if(paramTypes.length != types.length) continue;

            for(int i=0;i<paramTypes.length;i++){
                Class<?> t1 = types[i];
                Class<?> t2 = paramTypes[i];

                if(t1 == null && isPrimitive(t2)) continue outerLoop;
                if(t1 != t2) continue outerLoop;
            }

            return constructor;
        }

        return null;
    }

    private static Constructor<?> findEmptyConstructor(Class<?> cls){
        Constructor<?>[] constructors = cls.getDeclaredConstructors();
        for(Constructor<?> constructor : constructors){
            if(constructor.getParameterCount() == 0) return constructor;
        }
        return null;
    }

    private static Class<?> toPrimitive(Class<?> c){
        try {
            return (Class<?>) c.getField("TYPE").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

    private static boolean isPrimitive(Class<?> c){
        return toPrimitive(c) != null;
    }

}
