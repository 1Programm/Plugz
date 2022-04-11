package com.programm.plugz.test;

import com.programm.plugz.api.Service;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.lifecycle.PostStartup;
import com.programm.plugz.schedules.ISchedules;
import com.programm.plugz.schedules.Scheduled;
import com.programm.projects.ioutils.log.api.out.ILogger;

@Service
public class TestService {

    @Get private ILogger log;

    @PostStartup
    public void onStartup(){
        log.info("Started !");
    }

    @Scheduled(repeat = 500)
    public void test(@Get String apple, @Get ISchedules schedules){
        log.info("Apple: {}", apple);

        if(apple.equals("Braeburn")){
            schedules.stopSchedule();
        }
    }

}
