package com.programm.projects.plugz;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class PlugzBuilder {

    public static final String PROP_SCAN_PATHS = "plugins.paths.";
    public static final String PROP_SCAN_PACKAGE = "plugins.package.";
    public static final String PROP_CLS_ANNOTATIONS = "custom.annotations.";

    private static List<String> getListOf(Properties p, String s){
        List<String> list = new ArrayList<>();
        int i = 0;

        while(true) {
            String prop = p.getProperty(s + i);
            if(prop == null) break;
            list.add(prop);
            i++;
        }

        return list;
    }


    static Plugz fromConfigFile(String path){
        return fromConfigFile(new File(path));
    }

    static Plugz fromConfigFile(File file){
        if(!file.exists()) throw new ConfigFailedException("File [" + file.getAbsolutePath() + "] does not exist!");

        Properties properties = new Properties();
        try (FileReader fr = new FileReader(file))
        {
            properties.load(fr);
        }
        catch (FileNotFoundException e){
            throw new ConfigFailedException("File [" + file.getAbsolutePath() + "] does not exist!", e);
        }
        catch (IOException e){
            throw new ConfigFailedException("Exception while reading file [" + file.getAbsolutePath() + "] as properties!", e);
        }

        return fromProperties(properties);
    }

    static Plugz fromInputStream(InputStream is){
        Properties properties = new Properties();
        try {
            properties.load(is);
        } catch (IOException e){
            throw new ConfigFailedException("Exception while reading InputStream as properties!", e);
        }

        return fromProperties(properties);
    }

    @SuppressWarnings("unchecked")
    static Plugz fromProperties(Properties properties){

        List<String> _scanPaths = getListOf(properties, PROP_SCAN_PATHS);
        List<URL> scanPaths = new ArrayList<>();
        for(String path : _scanPaths){
            scanPaths.add(buildUrl(path));
        }

        Map<URL, String> basePackageMap = new HashMap<>();
        for(int i=0;i<_scanPaths.size();i++){
            String _basePackage = properties.getProperty(PROP_SCAN_PACKAGE + i);
            basePackageMap.put(scanPaths.get(i), _basePackage);
        }

        List<String> _clsAnnotations = getListOf(properties, PROP_CLS_ANNOTATIONS);
        List<Class<? extends Annotation>> clsAnnotations = new ArrayList<>();
        for(String clsAnnotation : _clsAnnotations){
            try {
                Class<?> cls = PlugzBuilder.class.getClassLoader().loadClass(clsAnnotation);
                if(!Annotation.class.isAssignableFrom(cls)){
                    throw new ConfigFailedException("Specified class is not an Annotation!");
                }
                clsAnnotations.add((Class<? extends Annotation>)cls);
            } catch (ClassNotFoundException e) {
                throw new ConfigFailedException("Could not find class [" + clsAnnotation + "] in ClassPath!", e);
            }
        }

        return new Plugz(scanPaths, basePackageMap, clsAnnotations);
    }

    private static URL buildUrl(String path){
        try {
            path = path.replaceFirst("^~", System.getProperty("user.home"));
            if (!path.matches(".*:.*")) {
                path = "file:" + path;
            }

            return new URL(path);
        } catch (MalformedURLException e) {
            throw new ConfigFailedException("Invalid URL: [" + path + "]", e);
        }
    }



    private final List<URL> scanPaths = new ArrayList<>();
    private final Map<URL, String> basePackageMap = new HashMap<>();
    private final List<Class<? extends Annotation>> clsAnnotations = new ArrayList<>();

    PlugzBuilder(){}

    public PlugzBuilder addScanPath(URL url){
        scanPaths.add(url);
        return this;
    }

    public PlugzBuilder addScanPathWithPackage(URL url, String basePackage){
        scanPaths.add(url);
        basePackageMap.put(url, basePackage);
        return this;
    }

    public PlugzBuilder addScanPath(String path){
        scanPaths.add(buildUrl(path));
        return this;
    }

    public PlugzBuilder addScanPathWithPackage(String path, String basePackage){
        URL url = buildUrl(path);
        scanPaths.add(url);
        basePackageMap.put(url, basePackage);
        return this;
    }

    @SafeVarargs
    public final PlugzBuilder addClassAnnotation(Class<? extends Annotation>... classes){
        clsAnnotations.addAll(Arrays.asList(classes));
        return this;
    }

    public Plugz build(){
        return new Plugz(scanPaths, basePackageMap, clsAnnotations);
    }

}
