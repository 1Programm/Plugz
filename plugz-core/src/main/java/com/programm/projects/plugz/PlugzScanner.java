package com.programm.projects.plugz;

import com.programm.projects.ioutils.log.api.out.ILogger;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class PlugzScanner {

    final Map<Class<? extends Annotation>, List<Class<?>>> foundAnnotationClasses = new HashMap<>();

    public void searchInUrl(ILogger log, URL url, String base, URLClassLoader cl, List<Class<? extends Annotation>> annotationClasses) throws ScanException {
        String fileName = url.getFile();

        if(fileName.endsWith(".jar")){
            searchInJar(log, url, base, cl, annotationClasses);
        }
        else if(url.getProtocol().equals("file")){
            File file = new File(url.getFile());
            searchInFolder(log, file, base, cl, annotationClasses);
        }
        else {
            throw new IllegalStateException("Not Implemented yet!");
        }
    }

    private void searchInJar(ILogger log, URL url, String base, URLClassLoader cl, List<Class<? extends Annotation>> annotationClasses) throws ScanException {
        if(base != null) base = base.replaceAll("\\.", "/");

        try (ZipInputStream zip = new ZipInputStream(url.openStream())) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory()) {
                    String name = entry.getName();

                    if(!name.endsWith(".class")) continue;
                    if(base != null && !name.startsWith(base)) continue;

                    name = name.substring(0, name.length() - ".class".length());
                    name = name.replaceAll("/", ".");

                    loadScanClassFromName(log, cl, annotationClasses, name);
                }
            }
        }
        catch (IOException e){
            throw new ScanException("Could not read input stream for url: [" + url + "]!", e);
        }
    }

    private void searchInFolder(ILogger log, File file, String base, URLClassLoader cl, List<Class<? extends Annotation>> annotationClasses) throws ScanException {
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
                    loadScanClassFromName(log, cl, annotationClasses, fullName);
                }
            }
        }

    }

    private void loadScanClassFromName(ILogger log, URLClassLoader cl, List<Class<? extends Annotation>> annotationClasses, String name) {
        try{
            Class<?> cls = cl.loadClass(name);

            for(Class<? extends Annotation> annotationClass : annotationClasses){
                if(cls.isAnnotationPresent(annotationClass)){
                    foundAnnotationClasses.computeIfAbsent(annotationClass, ac -> new ArrayList<>()).add(cls);
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
