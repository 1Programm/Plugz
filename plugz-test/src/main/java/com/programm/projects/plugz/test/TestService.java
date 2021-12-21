package com.programm.projects.plugz.test;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.magic.api.*;

@Service
@Logger
public class TestService {

    @Get
    private ILogger log;

    @Get
    private MyResource res;

    private int i;

    @PostSetup
    private void init(){
        log.info("Initial: {}", res);
    }

    @Scheduled(repeat = 1000)
    private void test(@Get ISchedules schedules){
        i++;
        log.info("{}", i);

        if(i >= 10){
            schedules.stopScheduler();
        }
    }

}
