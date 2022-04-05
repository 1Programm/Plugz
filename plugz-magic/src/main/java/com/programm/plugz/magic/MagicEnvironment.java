package com.programm.plugz.magic;

import com.programm.plugz.api.MagicSetupException;
import com.programm.plugz.inject.PlugzUrlClassScanner;
import com.programm.projects.ioutils.log.api.out.ILogger;

public class MagicEnvironment {

    public static MagicEnvironment Start() throws MagicSetupException {
        return Start(new String[0]);
    }

    public static MagicEnvironment Start(String... args) throws MagicSetupException {
        MagicEnvironment env =  MagicEnvironmentBuilder.create(args);
        env.prepare();
        env.startup();
        return env;
    }


    private final String basePackage;
    private final String[] initialArgs;

    private final ProxyLogger log = new ProxyLogger();
    private final PlugzUrlClassScanner scanner = new PlugzUrlClassScanner();
    private final ConfigurationManager configurations = new ConfigurationManager();

    public MagicEnvironment(String... args){
        this("", args);
    }

    public MagicEnvironment(String basePackage, String... args){
        this.basePackage = basePackage;
        this.initialArgs = args;
    }

    public void prepare() throws MagicSetupException {
        try {
            configurations.init(initialArgs);
        }
        catch (MagicSetupException e){
            throw new MagicSetupException("Failed to initialize configuration manager.", e);
        }
    }

    public void startup(){

    }

    public void shutdown(){
        
    }






    public void setLogger(ILogger log){
        this.log.setLogger(log);
    }

}
