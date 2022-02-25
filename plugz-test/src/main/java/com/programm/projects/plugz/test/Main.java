package com.programm.projects.plugz.test;

import com.programm.projects.ioutils.log.logger.ConfLogger;
import com.programm.projects.plugz.magic.MagicEnvironment;
import com.programm.projects.plugz.magic.api.Service;
import com.programm.projects.plugz.magic.api.schedules.Scheduled;

import java.io.IOException;

@Service
public class Main {

    public static void main(String[] args) throws IOException {
        MagicEnvironment env = new MagicEnvironment();
        env.setLogger(ConfLogger.get());
        env.startup();
        env.postSetup();
    }

    private int c = 0;

    @Scheduled(repeat = 1000, stopAfter = 10000)
    private void rep(){
        c++;
        System.out.println("" + c);
    }

}
