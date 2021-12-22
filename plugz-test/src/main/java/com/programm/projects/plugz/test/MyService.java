package com.programm.projects.plugz.test;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.plugz.magic.api.*;

@Service
public class MyService {

    private int i;

    @Get private ISchedules schedules;
    @Get private MyResource res;

    @PostSetup
    public void started(@Get ILogger log) {
        log.info("Is Started: " + res);
    }

    @Scheduled(repeat = 1000)
    public void test(@Get ILogger log){
        i++;
        log.info("Bla: " + i);

        if(i == 10){
            schedules.stopScheduler();
        }
    }

}
