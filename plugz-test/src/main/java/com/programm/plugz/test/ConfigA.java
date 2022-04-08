package com.programm.plugz.test;

import com.programm.plugz.api.Config;
import com.programm.plugz.api.auto.Get;
import com.programm.projects.ioutils.log.api.out.ILogger;

@Config
public class ConfigA {

    public ConfigA(@Get ILogger log, @Get(required = false) MyConfig c){
        log.info("{}", c);
    }

}
