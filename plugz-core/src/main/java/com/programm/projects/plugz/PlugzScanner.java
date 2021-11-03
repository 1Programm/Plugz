package com.programm.projects.plugz;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
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
        else {
            System.out.println("Not implemented yet!");
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

                    try {
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
            }
        }
        catch (IOException e){
            throw new ScanException("Could not read input stream for url: [" + url + "]!", e);
        }

    }

}
