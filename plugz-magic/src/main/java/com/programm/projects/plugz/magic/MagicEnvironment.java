package com.programm.projects.plugz.magic;

import com.programm.projects.plugz.Plugz;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;

public class MagicEnvironment {

    private final String basePackageOfCallingClass;
    private final Plugz plugz;
    private final MagicInstanceManager instanceManager;
    private final List<URL> additionalUrls = new ArrayList<>();
    private final Map<URL, String> additionalBases = new HashMap<>();
    private boolean disableCallingUrl;

    public MagicEnvironment(){
        this("");
    }

    public MagicEnvironment(String basePackage){
        this.basePackageOfCallingClass = basePackage;
        this.plugz = Plugz.create()
                .addClassAnnotation(Service.class)
                .build();
        this.instanceManager = new MagicInstanceManager();
    }

    public MagicEnvironment disableCallingUrl(){
        disableCallingUrl = true;
        return this;
    }

    public MagicEnvironment addUrl(URL url, String basePackage){
        additionalUrls.add(url);
        additionalBases.put(url, basePackage);
        return this;
    }

    public void startup(){
        startupScan();
        startupInstantiate();
    }

    private void startupScan(){
        List<URL> additionalUrls = new ArrayList<>(this.additionalUrls);
        Map<URL, String> bases = new HashMap<>(this.additionalBases);

        if(!disableCallingUrl) {
            StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
            String _callingClass = ste.getClassName();
            try {
                Class<?> callingClass = MagicEnvironment.class.getClassLoader().loadClass(_callingClass);
                URL url = callingClass.getProtectionDomain().getCodeSource().getLocation();
                additionalUrls.add(url);
                bases.put(url, basePackageOfCallingClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        plugz.scan(additionalUrls, bases, Collections.emptyList());
    }

    private void startupInstantiate(){
        List<Class<?>> serviceClasses = plugz.getAnnotatedWith(Service.class);

        if(serviceClasses != null) {
            try {
                for (Class<?> cls : serviceClasses) {
                    instanceManager.instantiate(cls);
                }
            } catch (MagicInstanceException e) {
                throw new IllegalStateException("Could not instantiate Service classes.", e);
            }
        }

        try {
            instanceManager.checkWaitMap();
        }
        catch (MagicInstanceException e){
            throw new IllegalStateException("Cyclic waiting dependencies could not be resolved!", e);
        }
    }

    public void postSetup(){
        try {
            instanceManager.callPostSetup();
        } catch (MagicInstanceException e){
            throw new IllegalStateException("Exception while calling post setup methods.", e);
        }
    }

    //TODO
    public void shutdown(){
        try {
            instanceManager.callPreShutdown();
        } catch (MagicInstanceException e){
            throw new IllegalStateException("Exception while calling pre shutdown.", e);
        }
    }

    public List<Class<?>> getAnnotatedWith(Class<? extends Annotation> cls){
        return plugz.getAnnotatedWith(cls);
    }

}
