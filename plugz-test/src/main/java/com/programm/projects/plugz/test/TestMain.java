package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.MagicEnvironment;

public class TestMain {

    public static void main(String[] args) {
        MagicEnvironment env = new MagicEnvironment("com.programm.projects.plugz.test");
        env.startup();
    }

}
