package com.programm.plugztest;

import com.programm.plugz.api.Service;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.lifecycle.PostStartup;
import com.programm.projects.ioutils.log.api.out.ILogger;

@Service
public class MyService {

    @Get private ILogger log;

    @PostStartup
    public void onStartup(){
        log.info("<<< Started >>>");
    }

}
