package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.Async;
import com.programm.plugz.api.Service;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.magic.MagicEnvironment;
import com.programm.plugz.schedules.Scheduled;

@Service
public class Main {

    public static void main(String[] args) throws Exception {
        MagicEnvironment.Start(args);
    }

    @Get private ILogger log;

    @Scheduled(repeat = 1000, stopAfter = 5000)
    @Async
    public void test1(){
        log.info("Test 1");
    }

    @Scheduled(repeat = 1000, stopAfter = 5000)
    public void test2(){
        log.info("Test 2");
    }
}
