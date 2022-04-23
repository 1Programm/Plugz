package com.programm.plugz.test;

import com.programm.plugz.api.Config;
import com.programm.plugz.api.auto.SetConfig;
import com.programm.plugz.magic.MagicEnvironment;

@Config
public class Main {

    @SetConfig("log.level")
    private static final String LOG_LEVEL = "TRACE";

    public static void main(String[] args) throws Exception {
        MagicEnvironment.Start(args);
    }

}
