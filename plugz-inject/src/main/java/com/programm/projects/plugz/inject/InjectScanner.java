package com.programm.projects.plugz.inject;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Logger("Inject-Scanner")
class InjectScanner {

    final Map<Class<?>, List<Class<?>>> implementationMap = new HashMap<>();
    private final ILogger log;
    private final URL scanUrl;
    private String scanPkg;
    boolean lighted;

    public InjectScanner(ILogger log, URL scanUrl) {
        this.log = log;
        this.scanUrl = scanUrl;
    }

    public void rescan(String pkg){
        implementationMap.clear();
        scan(pkg);
    }

    public void scan(String pkg){
        this.scanPkg = pkg;

        log.debug("Scanning for classes within package [{}] in [{}]", pkg, scanUrl);
        List<Class<?>> scannedClasses = getClassesInURL(scanUrl);
        log.debug("Found [{}] classes.", scannedClasses.size());

        for(Class<?> cls : scannedClasses){
            registerImplementations(cls, null);
        }

        lighted = true;
    }

    private void registerImplementations(Class<?> cls, Class<?> of){
        if(of == null) of = cls;

        Class<?>[] interfaces = cls.getInterfaces();

        for(Class<?> i : interfaces){
            registerImplementation(i, of);
        }

        Class<?> superCls = cls.getSuperclass();

        if(superCls != null && !superCls.isInterface() && superCls != Object.class){
            registerImplementation(superCls, of);
            registerImplementations(superCls, cls);
        }
    }

    private void registerImplementation(Class<?> i, Class<?> impl){
        log.trace("%70<({}) is an implementation of [{}]", impl, i);
        implementationMap.computeIfAbsent(i, in -> new ArrayList<>()).add(impl);
    }

    private List<Class<?>> getClassesInURL(URL url){
        if(url.getProtocol().equals("file")){
            log.trace("Scanning file url...");
            File dir = new File(url.getFile());
            return getClassesInDirForPackage(dir, scanPkg);
        }
        else if(url.getProtocol().equals("jar")){
            log.trace("Scanning jar url...");
            return getClassesInJar(url);
        }
        else {
            throw new IllegalArgumentException("Invalid protocol: [" + url.getProtocol() + "]!");
        }
    }

    private List<Class<?>> getClassesInDirForPackage(File dir, String pkg){
        List<Class<?>> classes = new ArrayList<>();

        if(dir.exists()){
            File[] children = dir.listFiles();

            if(children != null) {
                for (File child : children) {
                    String name = child.getName();

                    if (child.isDirectory()) {
                        List<Class<?>> nClasses = getClassesInDirForPackage(child, pkg + "." + name);
                        classes.addAll(nClasses);
                    }
                    else if(name.endsWith(".class")){
                        String clsName = pkg + "." + name.substring(0, name.length() - 6);
                        try {
                            Class<?> cls = Class.forName(clsName);
                            log.trace("# {}", cls);
                            classes.add(cls);
                        }
                        catch (ClassNotFoundException e){
                            log.error("Could not create class [{}]:", clsName);
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return classes;
    }

    private List<Class<?>> getClassesInJar(URL url){
        List<Class<?>> classes = new ArrayList<>();

        String pathToJar = url.getFile();
        int lastExclamationMark = pathToJar.lastIndexOf('!');
        pathToJar = pathToJar.substring("file:".length(), lastExclamationMark);

        String pkgPath = Utils.packageToPath(scanPkg);

        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(pathToJar))){
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory()) {
                    String name = entry.getName();

                    if(name.startsWith(pkgPath) && name.endsWith(".class")) {
                        // This ZipEntry represents a class. Now, what class does it represent?
                        String clsName = entry.getName().replace('/', '.'); // including ".class"
                        clsName = clsName.substring(0, clsName.length() - ".class".length());

                        try {
                            Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(clsName);
                            log.trace("# {}", cls);
                            classes.add(cls);
                        }
                        catch (ClassNotFoundException e){
                            log.error("Could not find a class in current ContextClassLoader with name: [{}]!", clsName);
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }

        return classes;
    }

}
