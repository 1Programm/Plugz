package com.programm.plugz.codegen;

import javax.tools.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

public class ProxyFactory {

    public static final String VISIBILITY_PACKAGE_PRIVATE = "";
    public static final String VISIBILITY_PRIVATE = "private";
    public static final String VISIBILITY_PROTECTED = "protected";
    public static final String VISIBILITY_PUBLIC = "public";

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<?> superClass, ProxyMethodHandler methodHandler, Object... constructorArgs) throws ProxyClassCreationException {
        String packageName = superClass.getPackageName();
        String className = superClass.getSimpleName();

        File sourceFile = createSourceFile(packageName, className);
        File parentDirectory = sourceFile.getParentFile();
        String generatedClassName = sourceFile.getName().split("\\.")[0];
        String fullGeneratedClassName = packageName + "." + generatedClassName;



        String sourceCode = JavaSourceCodeGenerator.generateJavaClassProxy(packageName, generatedClassName, superClass, VISIBILITY_PUBLIC);

        try {
            FileWriter writer = new FileWriter(sourceFile);
            writer.write(sourceCode);
            writer.close();
        }
        catch (IOException e){
            throw new ProxyClassCreationException("Failed to write source code into temporary file: " + sourceFile + "!", e);
        }






        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(parentDirectory));
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile));
            compiler.getTask(null, fileManager, null, null, null, compilationUnits).call();
            fileManager.close();
        }
        catch (IOException e){
            throw new ProxyClassCreationException("Failed to compile source code!", e);
        }



        URL sourceURL;
        try {
            sourceURL = parentDirectory.toURI().toURL();
        }
        catch (MalformedURLException e){
            throw new ProxyClassCreationException("Malformed url from generated source directory!", e);
        }

        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { sourceURL });


        Class<?> cls;
        try {
            cls = classLoader.loadClass(fullGeneratedClassName);
        }
        catch (ClassNotFoundException e){
            throw new ProxyClassCreationException("Failed to load class " + className + "!", e);
        }

        Class<?>[] constructorParamTypes = new Class[constructorArgs.length];
        for(int i=0;i<constructorArgs.length;i++){
            Object arg = constructorArgs[i];
            if(arg != null){
                constructorParamTypes[i] = arg.getClass();
            }
        }



        Constructor<?> con = findFittingConstructor(cls, constructorParamTypes);
        if(con == null) throw new ProxyClassCreationException("Could not find a fitting constructor for argument types: " + Arrays.toString(constructorParamTypes) + "!");

        Object proxyInstance;
        try {
            proxyInstance = con.newInstance(constructorArgs);
        }
        catch (InstantiationException | InvocationTargetException | IllegalAccessException e){
            throw new ProxyClassCreationException("Failed to call constructor!", e);
        }

        Field handlerField;
        try {
            handlerField = cls.getField(JavaSourceCodeGenerator.PROXY_FIELD_HANDLER);
        }
        catch (NoSuchFieldException e){
            throw new IllegalStateException("INVALID STATE: Should have generated a field with name: [" + JavaSourceCodeGenerator.PROXY_FIELD_HANDLER + "]!", e);
        }

        try {
            handlerField.set(proxyInstance, methodHandler);
        }
        catch (IllegalAccessException e){
            throw new IllegalStateException("INVALID STATE: The generated field [" + JavaSourceCodeGenerator.PROXY_FIELD_HANDLER + "] should be accessible!", e);
        }

        return (T) proxyInstance;
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

    private static File createSourceFile(String packageName, String className) throws ProxyClassCreationException{
        File sourceFile;
        try {
            File tmpDir = null;
            if(!packageName.isEmpty()) {
                tmpDir = FileManager.createTmpDirectory(packageName);
//                System.out.println("Sources Folder: " + tmpDir.getAbsolutePath());
            }

            sourceFile = FileManager.createTmpFile(className, ".java", tmpDir);
//            System.out.println("Source File: " + sourceFile.getAbsolutePath());
        }
        catch (IOException e){
            throw new ProxyClassCreationException("Failed to create temporary class [" + className + "]!", e);
        }

        return sourceFile;
    }

}
