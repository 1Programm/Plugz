package com.programm.projects.plugz.test;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.plugz.magic.api.*;

@Service
public class MyService {

    @Get private ILogger log;

    private long time;

    @PostSetup
    public void setup(){
        time = System.currentTimeMillis();

        log("Starting...");
    }

    @PostSetup
    @Async(delay = 1000)
    public void a(){
        log("a");
    }

    @PostSetup
    @Async(delay = 1100)
    public void b(){
        log("b");
    }

//    @Scheduled(repeat = 1000)
//    public void test() throws Exception{
//        log("Test Start.");
//        Thread.sleep(5000);
//        log("Test End.");
//    }

    private void log(String s){
        long t = System.currentTimeMillis() - time;
        log.info("[%4<({})]: {}", t, s);
    }





}
