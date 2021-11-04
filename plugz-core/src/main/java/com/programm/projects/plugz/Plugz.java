package com.programm.projects.plugz;

import com.programm.projects.ioutils.log.api.out.IOutput;
import com.programm.projects.ioutils.log.api.out.NullOutput;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class Plugz {

    private static final String STATIC_CONFIG_FILE_NAME = "plugz.properties";
    static IOutput STATIC_LOG = new NullOutput();

    public static void setLogger(IOutput logger){
        STATIC_LOG = logger;
    }

    public static Plugz fromConfigFile(){
        InputStream is = Plugz.class.getResourceAsStream("/" + STATIC_CONFIG_FILE_NAME);
        if(is == null) throw new IllegalStateException("No [" + STATIC_CONFIG_FILE_NAME + "] config file found in classPath!");
        return PlugzBuilder.fromInputStream(is);
    }

    public static Plugz fromConfigFile(String path){
        return PlugzBuilder.fromConfigFile(path);
    }

    public static Plugz fromConfigFile(File file){
        return PlugzBuilder.fromConfigFile(file);
    }

    public static PlugzBuilder create(){
        return new PlugzBuilder();
    }



    private final List<URL> baseScanPaths;
    private final Map<URL, String> basePackageMap;
    private final List<Class<? extends Annotation>> baseClsAnnotations;

    private final Map<Class<? extends Annotation>, List<Class<?>>> annotationClasses = new HashMap<>();

    public void scan(){
        scanUrls(baseScanPaths, basePackageMap, baseClsAnnotations);
    }

    public void scan(String... additionScanPaths){
        List<URL> allScanPaths = new ArrayList<>(baseScanPaths);

        for(String scanPath : additionScanPaths){
            try {
                allScanPaths.add(new URL(scanPath));
            }
            catch (MalformedURLException e){
                e.printStackTrace();
            }
        }

        scanUrls(allScanPaths, basePackageMap, baseClsAnnotations);
    }

    public void scan(List<URL> additionScanPaths, Map<URL, String> additionalBasePackageMap, List<Class<? extends Annotation>> additionalClsAnnotations){
        List<URL> allScanPaths = new ArrayList<>(baseScanPaths);
        allScanPaths.addAll(additionScanPaths);

        List<Class<? extends Annotation>> allClsAnnotations = new ArrayList<>(baseClsAnnotations);
        allClsAnnotations.addAll(additionalClsAnnotations);

        Map<URL, String> allBasePackageMap = new HashMap<>(basePackageMap);
        allBasePackageMap.putAll(additionalBasePackageMap);

        scanUrls(allScanPaths, allBasePackageMap, allClsAnnotations);
    }

    private void scanUrls(List<URL> scanPaths, Map<URL, String> basePackageMap, List<Class<? extends Annotation>> clsAnnotations){
        if(scanPaths.isEmpty()){
            STATIC_LOG.println("[WARN]: No scan paths defined.");
            return;
        }

        URL[] scanPathArray = scanPaths.toArray(new URL[0]);
        URLClassLoader classLoader = URLClassLoader.newInstance(scanPathArray);

        try {
            for (URL url : scanPathArray) {
                String basePackage = basePackageMap.getOrDefault(url, "");
                PlugzScanner.searchInUrl(url, basePackage, classLoader, clsAnnotations, annotationClasses);
            }
        }
        catch (ScanException e){
            throw new IllegalStateException("Failed to scan urls!", e);
        }
    }

    public List<Class<?>> getAnnotatedWith(Class<? extends Annotation> cls){
        return annotationClasses.get(cls);
    }

}
