package com.programm.projects.plugz.magic;

import com.programm.projects.plugz.Plugz;

import java.net.URL;
import java.util.*;

public class MagicEnvironment {

    private final String basePackageOfCallingClass;
    private final Plugz plugz;
    private final MagicInstanter magicInstanter;

    public MagicEnvironment(){
        this("");
    }

    public MagicEnvironment(String basePackage){
        this.basePackageOfCallingClass = basePackage;
        this.plugz = Plugz.create()
                .addClassAnnotation(Service.class)
                .build();
        this.magicInstanter = new MagicInstanter();
    }

    public void startup(){
        startupScan();
        startupInstantiate();
        startupPostSetup();
    }

    private void startupScan(){
        List<URL> additionalUrls = new ArrayList<>();
        Map<URL, String> bases = new HashMap<>();

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

        plugz.scan(additionalUrls, bases, Collections.emptyList());
    }

    private void startupInstantiate(){
        List<Class<?>> serviceClasses = plugz.getAnnotatedWith(Service.class);

        try {
            for (Class<?> cls : serviceClasses) {
                magicInstanter.instantiate(cls);
            }
        }
        catch (MagicInstanceException e){
            throw new IllegalStateException("Could not instantiate Service classes.", e);
        }
    }

    private void startupPostSetup(){
        try {
            magicInstanter.callPostSetup();
        } catch (MagicInstanceException e){
            throw new IllegalStateException("Exception while calling post setup methods.", e);
        }
    }

    //TODO
    public void shutdown(){
        try {
            magicInstanter.callPreShutdown();
        } catch (MagicInstanceException e){
            throw new IllegalStateException("Exception while calling pre shutdown.", e);
        }
    }

}
