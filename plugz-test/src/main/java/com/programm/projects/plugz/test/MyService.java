package com.programm.projects.plugz.test;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.plugz.magic.api.Get;
import com.programm.projects.plugz.magic.api.PostSetup;
import com.programm.projects.plugz.magic.api.Service;

@Service
public class MyService {

    @Get private ILogger log;
    @Get private TestRes res;

    @PostSetup
    public void setup(){
        log.info("Starting...");
        log.info("Hello: {}", res);
    }

}
