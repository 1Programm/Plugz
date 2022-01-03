package com.programm.projects.plugz.test;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.logger.ConfLogger;
import com.programm.projects.plugz.inject.InjectManager;

import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception{
        InjectManager manager = new InjectManager("com.programm.projects");
        manager.setLogger(ConfLogger.get());
        manager.scan();


        List<Class<? extends ILogger>> classes = manager.findImplementations(ILogger.class);

//        System.out.println(a);

//        MagicEnvironment env = new MagicEnvironment();
//        env.setLogger(ConfLogger.get());
//        env.startup();
//        env.postSetup();
    }
}
