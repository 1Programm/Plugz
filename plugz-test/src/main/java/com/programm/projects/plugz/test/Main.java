package com.programm.projects.plugz.test;

import com.programm.projects.ioutils.log.logger.ConfLogger;
import com.programm.projects.plugz.magic.MagicEnvironment;

public class Main {

    public static void main(String[] args) throws Exception{
        MagicEnvironment env = new MagicEnvironment();
        env.setLogger(ConfLogger.get());
        env.startup();
        env.postSetup();
    }

}
