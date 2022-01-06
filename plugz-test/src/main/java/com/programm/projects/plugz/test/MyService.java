package com.programm.projects.plugz.test;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.plugz.magic.api.*;
import com.programm.projects.plugz.magic.api.schedules.Scheduled;

import java.util.List;

@Service
public class MyService {

    @Get private ILogger log;
    @Get private UserRepo repo;

    @PostSetup
    public void zirst(){
        log.info("A");
        repo.create(1).setName("Peter");
        repo.create(2).setName("Gandalf");
        repo.create(3).setName("Peter");
        repo.create(4).setName("Udolf");
    }

    @PostSetup
    @Async(delay = 10)
    public void start(){
        log.info("ONSTART");

        List<TestUser> users = repo.findByName("Gandalf");

        log.info("User: {}", users);
    }

    @Scheduled(repeat = 2000, stopAfter = 10000)
    public void a(){
        log.info("a");
    }

}
