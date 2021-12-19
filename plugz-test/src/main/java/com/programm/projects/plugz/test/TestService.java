package com.programm.projects.plugz.test;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.magic.api.Get;
import com.programm.projects.plugz.magic.api.ISchedules;
import com.programm.projects.plugz.magic.api.Scheduled;
import com.programm.projects.plugz.magic.api.Service;

@Service
@Logger
public class TestService {

    @Get
    private ILogger log;

    private int i;

    @Scheduled(repeat = 1000)
    private void test(@Get ISchedules schedules){
        i++;
        log.info("" + i);

        if(i >= 10){
            schedules.stopScheduler();
        }
    }

}
