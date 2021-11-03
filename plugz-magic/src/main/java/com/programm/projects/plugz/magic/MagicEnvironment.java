package com.programm.projects.plugz.magic;

import com.programm.projects.plugz.Plugz;

import java.net.URL;
import java.util.*;

public class MagicEnvironment {

    private final String basePackageOfCallingClass;
    private final Plugz plugz;

    public MagicEnvironment(){
        this("");
    }

    public MagicEnvironment(String basePackage){
        this.basePackageOfCallingClass = basePackage;
        plugz = Plugz.create()
                .addClassAnnotation(Service.class)
                .build();
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

    }

    private void startupPostSetup(){

    }

}
