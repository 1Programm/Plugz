package com.programm.plugz.test;

import com.programm.plugz.api.Config;
import com.programm.plugz.api.auto.ConfigValue;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.auto.GetConfig;
import com.programm.projects.ioutils.log.api.out.ILogger;

@Config
public class MyConfig {

    public MyConfig(@Get ILogger log, @Get ConfigA a){
        log.info("{}", a);
    }

    @ConfigValue("test")
    private String setTestConfig(@Get ILogger log){
        return log.toString();
    }

    @GetConfig("test")
    private void getTestConfig(String test, @Get ILogger log){
        log.info("Config [test]: " + test);
    }

}
