package com.programm.plugz.inject;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.ioutils.log.api.NullLogger;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Logger("Plugz Scanner")
public class PlugzUrlClassScanner {

    private final List<String> blacklistedPackages = new ArrayList<>();
    private final List<String> tmpBlacklistedPackages = new ArrayList<>();

    private final Map<Class<? extends Annotation>, List<Class<?>>> foundAnnotatedClasses = new HashMap<>();
    private final Map<Class<?>, List<Class<?>>> foundImplementingClasses = new HashMap<>();

    private ILogger log = new NullLogger();

    public void scan(List<URL> searchUrls, String basePackage) throws ScanException {
        try {
            if (searchUrls == null || searchUrls.isEmpty()) {
                log.warn("# No urls to search through!");
                return;
            }
            if (basePackage == null) basePackage = "";

            for (int i = 0; i < searchUrls.size(); i++) {
                URL url = searchUrls.get(i);
                String urlFile = url.getFile();

                if (i != 0 && log.level() == ILogger.LEVEL_TRACE) log.debug("");

                if (urlFile.endsWith(".jar")) {
                    log.debug("# Scanning url as jar: [{}]...", url);
                    searchInJar(url, basePackage);
                } else if (url.getProtocol().equals("file")) {
                    log.debug("# Scanning url as class folder: [{}]...", url);
                    File file = new File(urlFile);
                    searchInFolder(file, basePackage);
                } else {
                    throw new ScanException("Could not scan url: [" + url + "]: Invalid url type!");
                }
            }
        }
        finally {
            tmpBlacklistedPackages.clear();
        }
    }

    public List<Class<?>> getAnnotatedWith(Class<? extends Annotation> cls){
        return foundAnnotatedClasses.get(cls);
    }

    public List<Class<?>> getImplementing(Class<?> cls){
        return foundImplementingClasses.get(cls);
    }

    public void addSearchAnnotation(Class<? extends Annotation> cls){
        this.foundAnnotatedClasses.putIfAbsent(cls, new ArrayList<>());
    }

    public void addSearchClass(Class<?> cls){
        this.foundImplementingClasses.putIfAbsent(cls, new ArrayList<>());
    }

    public void blacklistPackage(String pkg){
        blacklistedPackages.add(pkg);
    }

    public void blacklistPackageTemporary(String pkg){
        tmpBlacklistedPackages.add(pkg);
    }

    public void setLogger(ILogger log){
        this.log = log;
    }






    private void searchInJar(URL url, String basePackage) throws ScanException {
        basePackage = basePackage.replaceAll("\\.", "/");

        try (ZipInputStream zip = new ZipInputStream(url.openStream())) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory()) {
                    String name = entry.getName();

                    if(!name.endsWith(".class") || !name.startsWith(basePackage)) continue;

                    name = name.substring(0, name.length() - ".class".length());
                    name = name.replaceAll("/", ".");

                    if(isBlacklisted(name, false)){
                        log.trace("# Encountered blacklisted package [{}]", name);
                        continue;
                    }

                    loadAndScanClassFromName(name);
                }
            }
        }
        catch (IOException e){
            throw new ScanException("Could not read input stream for url: [" + url + "]!", e);
        }
    }

    private void searchInFolder(File file, String basePackage) throws ScanException {
        String rootFolderPath = file.getAbsolutePath();
        file = getFileForBasePackage(file, basePackage);
        if(file == null) return;

        Queue<File> toBeScanned = new ArrayDeque<>();
        toBeScanned.add(file);

        while(!toBeScanned.isEmpty()){
            File curFile = toBeScanned.poll();

            File[] children = curFile.listFiles();
            if(children != null){
                for(File child : children){
                    String relativePath = child.getAbsolutePath().substring(rootFolderPath.length() + 1);
                    if(isBlacklisted(relativePath, true)) {
                        log.trace("# Encountered blacklisted package [{}]", relativePath.replaceAll("/", "."));
                        continue;
                    }
                    toBeScanned.add(child);
                }
            }
            else {
                String name = curFile.getName();
                if(name.endsWith(".class")){
                    String fullName = getFullNameFromAbsolutePath(curFile.getAbsolutePath(), rootFolderPath);
                    loadAndScanClassFromName(fullName);
                }
            }
        }
    }

    private boolean isBlacklisted(String pkg, boolean isPath){
        for(String blacklistedPkg : blacklistedPackages){
            if(isPath) blacklistedPkg = blacklistedPkg.replaceAll("\\.", "/");
            if(pkg.startsWith(blacklistedPkg)) return true;
        }

        for(String blacklistedPkg : tmpBlacklistedPackages){
            if(isPath) blacklistedPkg = blacklistedPkg.replaceAll("\\.", "/");
            if(pkg.startsWith(blacklistedPkg)) return true;
        }

        return false;
    }

    private String getFullNameFromAbsolutePath(String path, String rootFolder){
        path = path.substring(rootFolder.length(), path.length() - ".class".length());
        if(path.startsWith("/")) path = path.substring(1);
        return path.replaceAll("/", ".");
    }

    private File getFileForBasePackage(File file, String basePackage) throws ScanException {
        int pos = 0;
        while(pos != -1 && pos < basePackage.length()){
            int nextDot = basePackage.indexOf('.', pos);
            String name;

            if(nextDot == -1){
                name = basePackage.substring(pos);
                pos = -1;
            }
            else {
                name = basePackage.substring(pos, nextDot);
                pos = nextDot + 1;
            }

            File nextFile = new File(file, name);
            if(!nextFile.exists() || !nextFile.isDirectory()) return null;

            file = nextFile;
        }

        return file;
    }

    private void loadAndScanClassFromName(String clsName) {
        Class<?> cls;

        try {
            cls = Class.forName(clsName);
        }
        catch (ClassNotFoundException e){
            log.error("# Something went wrong: Class [{}] could not be found!", clsName);
            e.printStackTrace();
            return;
        }
        catch (NoClassDefFoundError e){
            return;
        }

        log.trace("### Found class: [{}]", cls);

        Annotation[] annotations = cls.getDeclaredAnnotations();
        for(Annotation annotation : annotations){
            Class<?> annotationType = annotation.annotationType();
            List<Class<?>> registeredClasses = foundAnnotatedClasses.get(annotationType);
            if(registeredClasses != null && !registeredClasses.contains(cls)) {
                log.trace("###    %70<({}) is annotated with [{}]", cls, annotationType);
                registeredClasses.add(cls);
            }
        }

        tryToRegisterAsImplementations(cls, null);
    }

    private void tryToRegisterAsImplementations(Class<?> cls, Class<?> actualCls) {
        if(actualCls == null) {
            actualCls = cls;
        }
        else {
            List<Class<?>> registeredImplementations = foundImplementingClasses.get(cls);
            if(registeredImplementations != null && !registeredImplementations.contains(actualCls)) {
                log.trace("###    %70<({}) is an implementation of [{}]", actualCls, cls);
                registeredImplementations.add(actualCls);
            }
        }

        Class<?>[] interfaces = cls.getInterfaces();
        for(Class<?> iCls : interfaces){
            tryToRegisterAsImplementations(iCls, actualCls);
        }

        Class<?> superCls = cls.getSuperclass();
        if(superCls != null && superCls != Object.class){
            tryToRegisterAsImplementations(superCls, actualCls);
        }
    }

}
