package com.programm.plugz.test;

import com.programm.plugz.api.Service;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.lifecycle.*;
import com.programm.projects.ioutils.log.api.out.ILogger;

@Service
public class TestService {

    @PreSetup
    public void m1(@Get ILogger log){
        log.info("1");
    }

    @PostSetup
    public void m2(@Get ILogger log){
        log.info("2");
    }

    @PreStartup
    public void m3(@Get ILogger log){
        log.info("3");
    }

    @PostStartup
    public void m4(@Get ILogger log){
        log.info("4");
    }

    @PreShutdown
    public void m5(@Get ILogger log){
        log.info("5");
    }

    @PostShutdown
    public void m6(@Get ILogger log){
        log.info("6");
    }

}
