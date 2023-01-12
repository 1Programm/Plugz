package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.Service;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.lifecycle.PostStartup;
import com.programm.plugz.magic.MagicEnvironment;

import java.sql.Connection;
import java.util.List;

@Service
public class Application {

    public static void main(String[] args) throws Exception {
        MagicEnvironment.Start(args);
    }


    @Get private ILogger log;

    @PostStartup
    public void onStart(@Get PersonRepo repo) throws Exception {
//        Person person = repo.getById(1);
//        log.info("{}", person);

        Person p1 = repo.getById(1);
        p1.setName("Test Name");

        repo.update(p1);

//        List<Person> allPeople = repo.getAll();
//        System.out.println(allPeople);

//        Person p = new Person();
//        p.setName("Julian");
//        p.setAge(23);
//
//        repo.

//        Person p = repoHandler.createQuery("person.find(*, id = 0)", Person.class).execute();
//        log.info("{}", p);
    }

    @PostStartup
    public void onConnectionEstablished(@Get Connection connection){
        log.info("Connection Established! {}", connection);
    }
}
