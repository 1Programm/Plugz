package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.Config;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.lifecycle.PostSetup;
import com.programm.plugz.webserv.api.config.RestConfig;

@Config("app2")
public class App2 {

    @Get private ILogger log;

    @PostSetup
    public void onSetup(@Get RestConfig config){
        log.info("%30|[#](APP 2)");
    }

}
