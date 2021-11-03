package com.programm.projects.plugz;

import com.programm.projects.ioutils.log.api.out.IOutput;
import com.programm.projects.ioutils.log.api.out.NullOutput;

import java.io.File;
import java.io.InputStream;

public class Plugz {

    private static final String STATIC_CONFIG_FILE_NAME = "plugz.properties";
    static IOutput STATIC_LOG = new NullOutput();

    public static void setLogger(IOutput logger){
        STATIC_LOG = logger;
    }

    public static Plugz fromConfigFile(){
        InputStream is = Plugz.class.getResourceAsStream("/" + STATIC_CONFIG_FILE_NAME);
        if(is == null) throw new IllegalStateException("No [" + STATIC_CONFIG_FILE_NAME + "] config file found in classPath!");
        return PlugzBuilder.fromInputStream(is);
    }

    public static Plugz fromConfigFile(String path){
        return PlugzBuilder.fromConfigFile(path);
    }

    public static Plugz fromConfigFile(File file){
        return PlugzBuilder.fromConfigFile(file);
    }

    public static PlugzBuilder create(){
        return new PlugzBuilder();
    }



    private final int allowedWrapperTypes;

    Plugz(int allowedWrapperTypes){
        this.allowedWrapperTypes = allowedWrapperTypes;
    }



}
