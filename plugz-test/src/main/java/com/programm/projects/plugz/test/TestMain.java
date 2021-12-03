package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.api.IMagicMethod;
import com.programm.projects.plugz.magic.MagicEnvironment;
import com.programm.projects.plugz.magic.api.ISchedules;

public class TestMain {

    public static void main(String[] args) throws Exception {
        MagicEnvironment env = new MagicEnvironment();
        env.startup();

        Object instance = env.getInstance(BLA.class);
        if(instance == null) instance = env.instantiateClass(BLA.class);

        IMagicMethod method = env.createMagicMethod(instance, BLA.class.getDeclaredMethod("test", String.class, ISchedules.class));
        env.postSetup();

        method.call("bla");
    }

}
