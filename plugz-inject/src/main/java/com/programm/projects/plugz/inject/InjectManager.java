package com.programm.projects.plugz.inject;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.ioutils.log.api.out.NullLogger;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

@Logger("Inject-Manager")
public class InjectManager {

    private final List<URL> discoveredUrls = new ArrayList<>();
    private final Map<URL, InjectScanner> urlScannerMap = new HashMap<>();
    private final Map<Class<?>, List<URL>> cachedMappedImplementationUrls = new HashMap<>();
    private final String pkg;

    private ILogger log = new NullLogger();

    public InjectManager(String pkg) {
        this.pkg = pkg;
    }

    public void scan() throws IOException {
        log.info("Starting scan...");

        log.debug("Collecting dependencies and related urls...");
        collectUrls();
        log.debug("Found [{}] urls!", discoveredUrls.size());

        for(URL url : discoveredUrls){
            urlScannerMap.computeIfAbsent(url, u -> new InjectScanner(log, url)).scan(pkg);
        }
    }

    private void collectUrls() throws IOException {
        String path = Utils.packageToPath(pkg);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> urls = cl.getResources(path);

        while(urls.hasMoreElements()){
            URL url = urls.nextElement();
            log.trace("# Found [{}].", url);
            discoveredUrls.add(url);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<Class<? extends T>> findImplementations(Class<T> cls){
        List<URL> implementationUrls = cachedMappedImplementationUrls.get(cls);

        if(implementationUrls == null){
            implementationUrls = new ArrayList<>();
            cachedMappedImplementationUrls.put(cls, implementationUrls);

            List<Class<? extends T>> impls = new ArrayList<>();

            for(URL url : urlScannerMap.keySet()){
                InjectScanner scanner = urlScannerMap.get(url);
                List<Class<?>> nImpls = scanner.implementationMap.get(cls);

                if(nImpls == null || nImpls.size() == 0) continue;

                implementationUrls.add(url);

                for(Class<?> nCls : nImpls){
                    int mods = nCls.getModifiers();
                    if(!Modifier.isAbstract(mods)){
                        impls.add((Class<T>) nCls);
                    }
                }
            }

            return impls;
        }
        else {
            List<Class<? extends T>> impls = new ArrayList<>();

            for(URL url : implementationUrls){
                InjectScanner scanner = urlScannerMap.get(url);
                List<Class<?>> nImpls = scanner.implementationMap.get(cls);

                for(Class<?> nCls : nImpls){
                    int mods = nCls.getModifiers();
                    if(!Modifier.isAbstract(mods)){
                        impls.add((Class<T>) nCls);
                    }
                }
            }

            return impls;
        }
    }

    public <T> T findImpl(Class<T> cls){
        List<URL> implementationUrls = cachedMappedImplementationUrls.get(cls);

        if(implementationUrls == null){

        }

        return null;
    }

    public void setLogger(ILogger log){
        this.log = log;
    }

}
