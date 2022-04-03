package com.programm.projects.plugz.test;

import com.programm.projects.ioutils.log.logger.ConfLogger;
import com.programm.projects.plugz.magic.MagicEnvironment;
import com.programm.projects.plugz.magic.api.PostSetup;
import com.programm.projects.plugz.magic.api.Service;
import com.programm.projects.plugz.magic.api.debug.Debug;
import com.programm.projects.plugz.magic.api.debug.IValue;

@Service
public class Main {

    public static void main(String[] args) throws Exception {
        MagicEnvironment env = new MagicEnvironment(args);
        env.setLogger(ConfLogger.get());
        env.startup();
        env.postSetup();
    }

    @Debug
    private IValue<Integer> debugValue;

    @Debug
    private IValue<Boolean> val2;

    @Debug
    private IValue<String> val3;

    @Debug
    private IValue<Main> val4;

    @PostSetup
    public void test() throws Exception{
        System.out.println(debugValue);
        Thread.sleep(3000);
        debugValue.set((int)(Math.random() * 1000));
        Thread.sleep(3000);
        debugValue.set((int)(Math.random() * 1000));
        Thread.sleep(3000);
        debugValue.set((int)(Math.random() * 1000));
        Thread.sleep(3000);
        debugValue.set((int)(Math.random() * 1000));
        Thread.sleep(3000);
        debugValue.set((int)(Math.random() * 1000));
    }

}
