package com.programm.plugz.codegen;

import com.programm.plugz.codegen.codegenerator.JavaCodeGenerationException;
import com.programm.plugz.codegen.codegenerator.JavaCodeGenerator;

import javax.tools.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaCode {

    private static final JavaCodeGenerator codeGenerator = new JavaCodeGenerator();
    private static final Map<String, File> packageNameToTempDir = new HashMap<>();


    public static Class<?> createAndCompileClass(String packageName, String className, GeneratorConsumer setup) throws JavaCodeGenerationException {
        if(packageName == null) packageName = "";

        File sourceFile = writeCodeToFile(packageName, className, setup);
        File parentDirectory = sourceFile.getParentFile();
        String generatedClassName = sourceFile.getName().split("\\.")[0];
        String fullGeneratedClassName = packageName.isEmpty() ? generatedClassName : packageName + "." + generatedClassName;


        try {
            compileSourceCode(sourceFile, parentDirectory);
        }
        catch (IOException e){
            throw new JavaCodeGenerationException("Failed to compile source code!", e);
        }


        URL sourceURL;
        try {
            sourceURL = parentDirectory.toURI().toURL();
        }
        catch (MalformedURLException e){
            throw new JavaCodeGenerationException("Malformed url from generated source directory!", e);
        }

        try {
            return loadSingleClassFromUrl(sourceURL, fullGeneratedClassName);
        }
        catch (ClassNotFoundException e){
            throw new JavaCodeGenerationException("Failed to load class " + className + "!", e);
        }
    }

    public static File writeCodeToFile(String packageName, String className, GeneratorConsumer setup) throws JavaCodeGenerationException {
        if(packageName == null) packageName = "";

        File sourceFile = createSourceFile(packageName, className);
        String generatedClassName = sourceFile.getName().split("\\.")[0];

        setup.accept(codeGenerator, generatedClassName);
        String sourceCode = codeGenerator.build();

        try {
            FileWriter writer = new FileWriter(sourceFile);
            writer.write(sourceCode);
            writer.close();
        }
        catch (IOException e){
            throw new JavaCodeGenerationException("Failed to write source code into temporary file: " + sourceFile + "!", e);
        }

        return sourceFile;
    }

    public static void compileSourceCode(File sourceFile, File outputDir) throws IOException{
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir));
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile));
        compiler.getTask(null, fileManager, null, null, null, compilationUnits).call();
        fileManager.close();
    }

    public static Class<?> loadSingleClassFromUrl(URL url, String fullClassName) throws ClassNotFoundException {
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { url });
        return classLoader.loadClass(fullClassName);
    }

    public static Class<?>[] loadClassesFromUrl(URL[] urls, String... classNames) throws ClassNotFoundException {
        URLClassLoader classLoader = URLClassLoader.newInstance(urls);

        Class<?>[] classes = new Class[classNames.length];
        for(int i=0;i<classNames.length;i++){
            classes[i] = classLoader.loadClass(classNames[i]);
        }

        return classes;
    }

    private static File createSourceFile(String packageName, String className) throws JavaCodeGenerationException {
        File sourceFile;
        try {
            File tmpDir = packageNameToTempDir.get(packageName);
            if(tmpDir == null && !packageName.isEmpty()) {
                tmpDir = TmpFileManager.createTmpDirectory(packageName);
                packageNameToTempDir.put(packageName, tmpDir);
            }

            sourceFile = TmpFileManager.createTmpFile(className, ".java", tmpDir);
        }
        catch (IOException e){
            throw new JavaCodeGenerationException("Failed to create temporary class [" + className + "]!", e);
        }

        return sourceFile;
    }
}