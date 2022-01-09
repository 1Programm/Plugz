package com.programm.projects.plugz.test;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.plugz.magic.api.*;
import com.programm.projects.plugz.magic.api.schedules.Scheduled;

import java.util.List;

@Service
public class MyService {

    @Get private ILogger log;
    @Get private UserRepo repo;

    private TestUser user;

    @PostSetup
    public void zirst(){
        log.info("A");
        if(repo != null) {
            TestUser u1 = new TestUser(99);
            u1.setName("A");
            repo.save(u1);

            TestUser a1 = new TestUser(99);
            a1.setName("B");
            repo.save(a1);

            a1.setName("Peter");
            user = repo.save(a1);

            TestUser u2 = new TestUser(10);
            u2.setName("Gandalf");
            repo.save(u2);

            TestUser u3 = new TestUser(50);
            u3.setName("Peter");
            repo.save(u3);
        }
    }

    @PostSetup
    @Async(delay = 10)
    public void start(){
        log.info("ONSTART");

        if(repo != null) {
            List<TestUser> users1 = repo.findAll();
            log.info("User: {}", users1);

//            TestUser toRemove = new TestUser(99);
            repo.remove(user);

            List<TestUser> users2 = repo.findAll();
            log.info("User: {}", users2);
        }
    }

    @Scheduled(repeat = 2000, stopAfter = 10000)
    public void a(){
        log.info("a");
    }

}
