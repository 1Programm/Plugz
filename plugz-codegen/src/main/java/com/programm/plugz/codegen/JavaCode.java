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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to creating class files, compiling code and loading classes from generated code files.
 */
public class JavaCode {

    private static final JavaCodeGenerator codeGenerator = new JavaCodeGenerator();

    /**
     * Creates a new generated class defined by the setup consumer.
     * @param packageName the package name for the class.
     * @param className the class name which should be used as a base for the generated class name.
     * @param setup the setup consumer to generate the source code.
     * @return a generated class.
     * @throws JavaCodeGenerationException if the setup consumer throws some exception, the compiler throws an exception or the class could not be loaded.
     */
    public static Class<?> createAndCompileClass(File parentDirectory, String packageName, String className, GeneratorConsumer setup) throws JavaCodeGenerationException {
        if(packageName == null) packageName = "";

        File sourceFile = writeCodeToFile(parentDirectory, className, setup);
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

    /**
     * Creates a temp file and writes source code to it provided by the setup consumer.
     * @param parentFolder the folder to put the source file into.
     * @param className the class name.
     * @param setup the setup consumer to generate the source code.
     * @return a file where the source code was written to.
     * @throws JavaCodeGenerationException if the setup consumer throws some exception or some io exception was thrown.
     */
    public static File writeCodeToFile(File parentFolder, String className, GeneratorConsumer setup) throws JavaCodeGenerationException {
        if(className.length() < 3) {
            className += "_".repeat(3 - className.length());
        }

        File sourceFile;
        try {
            sourceFile = TmpFileManager.createTmpFile(className, ".java", parentFolder, false);
        }
        catch (IOException e){
            throw new JavaCodeGenerationException("Failed to create temporary class [" + className + "]!", e);
        }

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

    /**
     * Compiles some java source file and writes the .class file to the output directory.
     * @param sourceFile the source code file.
     * @param outputDir the desired output file.
     * @throws IOException if some io exception was thrown.
     */
    public static void compileSourceCode(File sourceFile, File outputDir) throws IOException{
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir));
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile));
        compiler.getTask(null, fileManager, null, null, null, compilationUnits).call();
        fileManager.close();
    }

    /**
     * Compiles multiple java source files and writes the .class file to the output directory.
     * @param sourceFiles the source code files.
     * @param outputDir the desired output file.
     * @throws IOException if some io exception was thrown.
     */
    public static void compileSourceCode(File[] sourceFiles, File outputDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir));
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFiles));
        compiler.getTask(null, fileManager, null, null, null, compilationUnits).call();
        fileManager.close();
    }

    /**
     * Creates a URLClassLoader and loads some class by its fully quallified name.
     * This method should be used with caution as creating a classloader and loading only a
     * single class can be inefficient when calling this method in a loop.
     * @param url the url where the class that should be loaded is located.
     * @param fullClassName the fully quallified class name.
     * @return the class.
     * @throws ClassNotFoundException if the class could not be found.
     */
    public static Class<?> loadSingleClassFromUrl(URL url, String fullClassName) throws ClassNotFoundException {
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { url });
        return classLoader.loadClass(fullClassName);
    }

    /**
     * Creates a URLClassLoader and loads classes by its fully quallified name.
     * This method should be more efficient when loading multiple classes in contrast to the {@link #loadSingleClassFromUrl(URL, String)} method.
     * @param urls the urls where the classes that should be loaded are located.
     * @param fullClassNames the fully quallified class names.
     * @return the class.
     * @throws ClassNotFoundException if the class could not be found.
     */
    public static Class<?>[] loadClassesFromUrl(URL[] urls, String... fullClassNames) throws ClassNotFoundException {
        URLClassLoader classLoader = URLClassLoader.newInstance(urls);

        Class<?>[] classes = new Class[fullClassNames.length];
        for(int i=0;i<fullClassNames.length;i++){
            classes[i] = classLoader.loadClass(fullClassNames[i]);
        }

        return classes;
    }
}