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

/**
 * Class for creating proxies.
 */
public class ProxyFactory {

    private static final String PROXY_FIELD_HANDLER = "$handler";
    private static final String GENERATED_CLASSES_FOLDER_NAME = "com.programm.plugz.codegen";
    private static final Map<Class<?>, Class<?>> CASHED_LOG_PROXY_CLASS_MAP = new HashMap<>();
    private static final Map<Class<?>, Class<?>> CASHED_PROXY_CLASS_MAP = new HashMap<>();

    private static boolean doCaching = true;

    /**
     * Enable or disable caching for future calls.
     * @param caching boolean flag.
     */
    public static void doCaching(boolean caching){
        doCaching = caching;
    }


    /**
     * Creates a log proxy class for some superclass.
     * @param superClass the superclass.
     * @return the new generated class with the superclass being its parent.
     * @throws ProxyClassCreationException if the class could not be created.
     */
    public static Class<?> createLogProxyClass(Class<?> superClass) throws ProxyClassCreationException {
        Class<?> cls = doCaching ? CASHED_LOG_PROXY_CLASS_MAP.get(superClass) : null;
        if(cls == null) {
            String superClassCanonicalName = superClass.getCanonicalName();
            if(superClassCanonicalName == null) throw new ProxyClassCreationException("Superclass cannot be a local, anonymous or hidden class [" + superClass + "]!");

            String packageName = superClass.getPackageName();
            String className = superClass.getSimpleName();

            File parentDirectory;
            try {
                parentDirectory = TmpFileManager.createTmpDirectory(GENERATED_CLASSES_FOLDER_NAME, true);
            }
            catch (IOException e){
                throw new ProxyClassCreationException("Failed to create parent source directory for generating classes to!", e);
            }

            try {
                cls = JavaCode.createAndCompileClass(parentDirectory, packageName, className, (g, name) -> {
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

    /**
     * Creates a log proxy instance for some superclass.
     * Every method call to that instance will do a System.out.println(...) before calling the method of the superclass.
     * @param superClass the superclass.
     * @return the proxy instance.
     * @throws ProxyClassCreationException if the class could not be created.
     */
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

    /**
     * Creates a log proxy class for some superclass.
     * @param superClass the superclass.
     * @return the new generated class with the superclass being its parent.
     * @throws ProxyClassCreationException if the class could not be created.
     */
    public static Class<?> createProxyClass(Class<?> superClass) throws ProxyClassCreationException {
        Class<?> cls = doCaching ? CASHED_PROXY_CLASS_MAP.get(superClass) : null;

        if(cls == null){
            String superClassCanonicalName = superClass.getCanonicalName();
            if(superClassCanonicalName == null) throw new ProxyClassCreationException("Superclass cannot be a local, anonymous or hidden class [" + superClass + "]!");

            String packageName = superClass.getPackageName();
            String className = superClass.getSimpleName();

            File parentDirectory;
            try {
                parentDirectory = TmpFileManager.createTmpDirectory(GENERATED_CLASSES_FOLDER_NAME, true);
            }
            catch (IOException e){
                throw new ProxyClassCreationException("Failed to create parent source directory for generating classes to!", e);
            }

            try {
                cls = JavaCode.createAndCompileClass(parentDirectory, packageName, className, (g, name) -> {
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

    /**
     * Creates a proxy instance with a method handler.
     * The method handler will be called before calling some method.
     * It will try to find a fitting constructor for the given args and call it.
     * @param superClass the superclass.
     * @param methodHandler the proxy method handler.
     * @param constructorArgs the args to pass to the constructor.
     * @return the proxy instance.
     * @throws ProxyClassCreationException if the creation of the proxy class failed.
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<? super T> superClass, ProxyMethodHandler methodHandler, Object... constructorArgs) throws ProxyClassCreationException {
        Class<?> cls = createProxyClass(superClass);
        Constructor<?> con = findFittingConstructorFromArgs(cls, constructorArgs);
        if(con == null) throw new ProxyClassCreationException("Could not find a fitting constructor for arguments: " + Arrays.toString(constructorArgs) + "!");

        Object proxyInstance = createAndSetupProxyHandler(cls, con, methodHandler, constructorArgs);
        return (T) proxyInstance;
    }

    /**
     * Creates multiple proxies with the same method handler.
     * Should be more efficient than the {@link #createProxy(Class, ProxyMethodHandler, Object...)} method.
     * @param methodHandler the proxy method handler.
     * @param superClasses the superclass.
     * @return the proxy instances.
     * @throws ProxyClassCreationException if the creation of the proxy class failed.
     */
    public static Object[] createMultipleProxies(ProxyMethodHandler methodHandler, Class<?>... superClasses) throws ProxyClassCreationException {
        int len = superClasses.length;

        Class<?>[] proxyClasses = new Class[len];
        List<File> toCompileSourceCodeFiles = new ArrayList<>();
        List<String> toCompileClassNames = new ArrayList<>();

        File parentDirectory = null;
        URL parentDirectoryUrl = null;

        int lenToCompile = 0;
        for(int i=0;i<len;i++){
            Class<?> superClass = superClasses[i];
            Class<?> cls = doCaching ? CASHED_PROXY_CLASS_MAP.get(superClass) : null;

            if(cls != null) {
                proxyClasses[i] = cls;
                continue;
            }

            if(parentDirectory == null){
                try {
                    parentDirectory = TmpFileManager.createTmpDirectory(GENERATED_CLASSES_FOLDER_NAME, true);
                }
                catch (IOException e){
                    throw new ProxyClassCreationException("Failed to create tmp parent directory for proxy classes", e);
                }

                try {
                    parentDirectoryUrl = parentDirectory.toURI().toURL();
                }
                catch (MalformedURLException e){
                    throw new ProxyClassCreationException("Malformed url from generated parent directory!", e);
                }
            }

            String superClassCanonicalName = superClass.getCanonicalName();
            if(superClassCanonicalName == null) throw new ProxyClassCreationException("Superclass cannot be a local, anonymous or hidden class [" + superClass + "]!");

            String packageName = superClass.getPackageName();
            String className = superClass.getSimpleName();

            try {
                File sourceCodeFile = JavaCode.writeCodeToFile(parentDirectory, className, (g, name) -> {
                    if(!packageName.isEmpty()) g.definePackage(packageName);
                    g.defineClass(name);
                    g.defineExtends(superClassCanonicalName);
                    generateProxyClass(g, superClass);
                });

                String generatedClassName = sourceCodeFile.getName().split("\\.")[0];
                String fullGeneratedClassName = packageName.isEmpty() ? generatedClassName : packageName + "." + generatedClassName;


                toCompileSourceCodeFiles.add(sourceCodeFile);
                toCompileClassNames.add(fullGeneratedClassName);
            }
            catch (JavaCodeGenerationException e){
                throw new ProxyClassCreationException("Failed to create source code file for class: [" + className + "]!", e);
            }

            lenToCompile++;
        }

        if(lenToCompile > 0){
            File[] sourceFiles = toCompileSourceCodeFiles.toArray(new File[0]);

            try {
                JavaCode.compileSourceCode(sourceFiles, parentDirectory);
            }
            catch (IOException e){
                throw new ProxyClassCreationException("Failed to compile source files!", e);
            }

            try {
                String[] classNamesToCompile = toCompileClassNames.toArray(new String[0]);
                Class<?>[] compiledClasses = JavaCode.loadClassesFromUrl(new URL[]{parentDirectoryUrl}, classNamesToCompile);

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
