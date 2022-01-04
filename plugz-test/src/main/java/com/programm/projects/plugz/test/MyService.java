package com.programm.projects.plugz.test;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.plugz.magic.api.Get;
import com.programm.projects.plugz.magic.api.PostSetup;
import com.programm.projects.plugz.magic.api.Service;
import com.programm.projects.plugz.magic.api.schedules.Scheduled;

@Service
public class MyService {

    @Get private ILogger log;
    @Get private UserRepo repo;

    @PostSetup
    public void start(){
        log.info("ONSTART");
        TestUser user = repo.getById(10);
        log.info("User: {}", user);
    }

    @Scheduled(repeat = 2000, stopAfter = 10000)
    public void a(){
        log.info("a");
    }

}
