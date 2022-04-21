package com.programm.plugztest;

import com.programm.plugz.api.Config;
import com.programm.plugz.api.auto.SetConfig;

@Config
public class MyConfig {

    @SetConfig("log.format")
    private static final String LOG_FORMAT = "%5<($LVL): $MSG";

}
