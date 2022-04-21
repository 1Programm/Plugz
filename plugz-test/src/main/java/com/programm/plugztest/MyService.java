package com.programm.plugztest;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.Service;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.lifecycle.PostStartup;

@Service
public class MyService {

    @Get private ILogger log;

    @PostStartup
    public void onStartup(){
        log.info("<<< Started >>>");
    }

}
