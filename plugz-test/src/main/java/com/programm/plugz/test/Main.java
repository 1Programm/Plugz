package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.IAsyncManager;
import com.programm.plugz.api.Service;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.debugger.DebugValue;
import com.programm.plugz.magic.MagicEnvironment;
import com.programm.plugz.schedules.ISchedules;
import com.programm.plugz.schedules.Scheduled;

@Service
public class Main {

    private static class Test {
        int a, b;
        String c, d;
        Test e;
    }

    public static void main(String[] args) throws Exception {
        MagicEnvironment.Start(args);
    }

    @Get private ILogger log;

    @DebugValue
    Test test = createTest();

    @DebugValue
    int a;

    @Scheduled(repeat = 1000)
    public void test(@Get ISchedules schedules){
        if(test.a == -1){
            schedules.stopSchedule();
            return;
        }

        log.info("{}", test.a);
        test.a++;
    }

    private static Test createTest(){
        Test test = new Test();
        test.a = 10;
        test.e = new Test();
        return test;
    }
}
