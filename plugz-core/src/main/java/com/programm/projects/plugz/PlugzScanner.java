package com.programm.projects.plugz;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class PlugzScanner {

    public static void searchInUrl(URL url, String base, URLClassLoader cl, List<Class<? extends Annotation>> annotationClasses, Map<Class<? extends Annotation>, List<Class<?>>> map) throws ScanException {
        String fileName = url.getFile();

        if(fileName.endsWith(".jar")){
            searchInJar(url, base, cl, annotationClasses, map);
        }
        else if(url.getProtocol().equals("file")){
            File file = new File(url.getFile());
            searchInFolder(file, base, cl, annotationClasses, map);
        }
        else {
            throw new IllegalStateException("Not Implemented yet!");
        }
    }

    private static void searchInJar(URL url, String base, URLClassLoader cl, List<Class<? extends Annotation>> annotationClasses, Map<Class<? extends Annotation>, List<Class<?>>> map) throws ScanException {
        if(base != null) base = base.replaceAll("\\.", "/");

        try (ZipInputStream zip = new ZipInputStream(url.openStream())) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory()) {
                    String name = entry.getName();

                    if(!name.endsWith(".class")) continue;
                    if(base != null && !name.startsWith(base)) continue;

                    name = name.substring(0, name.length() - ".class".length());
                    name = name.replaceAll("/", ".");

                    loadScanClassFromName(cl, annotationClasses, map, name);
                }
            }
        }
        catch (IOException e){
            throw new ScanException("Could not read input stream for url: [" + url + "]!", e);
        }
    }

    private static void searchInFolder(File file, String base, URLClassLoader cl, List<Class<? extends Annotation>> annotationClasses, Map<Class<? extends Annotation>, List<Class<?>>> map) throws ScanException {
        String rootFolder = file.getAbsolutePath();
        File cur = file;

        while(!base.equals("")){
            int nextDot = base.indexOf('.');
            if(nextDot == -1) nextDot = base.length();

            String name = base.substring(0, nextDot);
            base = base.substring(nextDot + 1);

            File[] children = file.listFiles();
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
                    loadScanClassFromName(cl, annotationClasses, map, fullName);
                }
            }
        }

    }

    private static void loadScanClassFromName(URLClassLoader cl, List<Class<? extends Annotation>> annotationClasses, Map<Class<? extends Annotation>, List<Class<?>>> map, String name) {
        try{
            Class<?> cls = cl.loadClass(name);

            for(Class<? extends Annotation> annotationClass : annotationClasses){
                if(cls.isAnnotationPresent(annotationClass)){
                    map.computeIfAbsent(annotationClass, ac -> new ArrayList<>()).add(cls);
                }
            }
        } catch (ClassNotFoundException e) {
            Plugz.STATIC_LOG.println("[ERROR]: Something went wrong: Class [" + name + "] could not be found!");
            e.printStackTrace();
        }
    }

    private static String getFullNameFromAbsolutePath(String path, String rootFolder){
        path = path.substring(rootFolder.length(), path.length() - ".class".length());
        if(path.startsWith("/")) path = path.substring(1);
        return path.replaceAll("/", ".");
    }
}
