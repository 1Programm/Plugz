package com.programm.projects.plugz;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Logger("Plugz-Scanner")
class PlugzScanner {

    final Map<Class<? extends Annotation>, List<Class<?>>> foundAnnotationClasses = new HashMap<>();

    public Map<Class<? extends Annotation>, List<Class<?>>> searchInUrl(ILogger log, URL url, String base, URLClassLoader cl, List<Class<? extends Annotation>> annotationClasses) throws ScanException {
        Map<Class<? extends Annotation>, List<Class<?>>> newAnnotatedClasses = new HashMap<>();

        String fileName = url.getFile();

        if(fileName.endsWith(".jar")){
            log.trace("Scanning [jar]...");
            searchInJar(log, url, base, cl, annotationClasses, newAnnotatedClasses);
        }
        else if(url.getProtocol().equals("file")){
            log.trace("Scanning [class folder]...");
            File file = new File(url.getFile());
            searchInFolder(log, file, base, cl, annotationClasses, newAnnotatedClasses);
        }
        else {
            throw new IllegalStateException("Not Implemented yet!");
        }

        return newAnnotatedClasses;
    }

    private void searchInJar(ILogger log, URL url, String base, URLClassLoader cl, List<Class<? extends Annotation>> annotationClasses, Map<Class<? extends Annotation>, List<Class<?>>> newAnnotatedClasses) throws ScanException {
        if(base != null) base = base.replaceAll("\\.", "/");

        try (ZipInputStream zip = new ZipInputStream(url.openStream())) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory()) {
                    String name = entry.getName();

                    if(!name.endsWith(".class")) continue;
                    if(base != null && !name.startsWith(base)) continue;

                    name = name.substring(0, name.length() - ".class".length());
                    name = name.replaceAll("/", ".");

                    log.trace("# Found class: [{}]", name);
                    loadScanClassFromName(log, cl, annotationClasses, name, newAnnotatedClasses);
                }
            }
        }
        catch (IOException e){
            throw new ScanException("Could not read input stream for url: [" + url + "]!", e);
        }
    }

    private void searchInFolder(ILogger log, File file, String base, URLClassLoader cl, List<Class<? extends Annotation>> annotationClasses, Map<Class<? extends Annotation>, List<Class<?>>> newAnnotatedClasses) throws ScanException {
        String rootFolder = file.getAbsolutePath();
        File cur = file;

        while(!base.equals("")){
            int nextDot = base.indexOf('.');
            String name;

            if(nextDot == -1) {
                name = base;
                base = "";
            }
            else {
                name = base.substring(0, nextDot);
                base = base.substring(nextDot + 1);
            }

            File[] children = cur.listFiles();
            if(children == null) throw new ScanException("Base package does not map with file path!");

            for(int i=0;i<children.length;i++){
                if(children[i].getName().equals(name)){
                    cur = children[i];
                    break;
                }

                if(i + 1 == children.length){
                    throw new ScanException("Base package does not map with file path!");
                }
            }
        }

        List<File> work = new ArrayList<>();
        work.add(cur);

        while(!work.isEmpty()){
            File f = work.remove(0);

            File[] children = f.listFiles();
            if(children != null){
                work.addAll(Arrays.asList(children));
            }
            else {
                String name = f.getName();
                if(name.endsWith(".class")){
                    String fullName = getFullNameFromAbsolutePath(f.getAbsolutePath(), rootFolder);

                    log.trace("# Found class: [{}]", fullName);
                    loadScanClassFromName(log, cl, annotationClasses, fullName, newAnnotatedClasses);
                }
            }
        }

    }

    private void loadScanClassFromName(ILogger log, URLClassLoader cl, List<Class<? extends Annotation>> annotationClasses, String name, Map<Class<? extends Annotation>, List<Class<?>>> newAnnotatedClasses) {
        try{
            Class<?> cls = cl.loadClass(name);

            for(Class<? extends Annotation> annotationClass : annotationClasses){
                if(cls.isAnnotationPresent(annotationClass)){
                    List<Class<?>> classes = foundAnnotationClasses.computeIfAbsent(annotationClass, ac -> new ArrayList<>());

                    if(!classes.contains(cls)){
                        log.trace("### Is Annotated with: [{}].", annotationClass.getSimpleName());
                        classes.add(cls);
                        newAnnotatedClasses.computeIfAbsent(annotationClass, c -> new ArrayList<>()).add(cls);
                    }
                    else {
                        log.trace("### Is Annotated with: [{}] but was already found.", annotationClass.getSimpleName());
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            log.error("Something went wrong: Class [{}] could not be found!", name);
            e.printStackTrace();
        }
    }

    private String getFullNameFromAbsolutePath(String path, String rootFolder){
        path = path.substring(rootFolder.length(), path.length() - ".class".length());
        if(path.startsWith("/")) path = path.substring(1);
        return path.replaceAll("/", ".");
    }
}
