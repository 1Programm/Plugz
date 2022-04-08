package com.programm.plugz.test;

import com.programm.plugz.magic.MagicEnvironment;
import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.logger.ConfLogger;

public class Main {

    public static void main(String[] args) throws Exception {
        MagicEnvironment env = new MagicEnvironment(args);
        env.setLogger(new ConfLogger("[%5<($LVL)] [%30>($LOG?{$CLS.$MET})]: $MSG", ILogger.LEVEL_INFO));
        env.setup();
        env.startup();
    }

}
