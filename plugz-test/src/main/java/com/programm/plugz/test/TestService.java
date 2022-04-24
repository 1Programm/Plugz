package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.Service;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.debugger.DebugValue;
import com.programm.plugz.schedules.Scheduled;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Service
public class TestService {

    @AllArgsConstructor
    @NoArgsConstructor
    private static class Test {
        int a, b;
        String c, d;
        Test e;
    }

    @DebugValue
    Test test = new Test(10, 0, null, null, new Test());

    @DebugValue
    int a;

    @Scheduled(repeat = 1000)
    public void test(@Get ILogger log){
        test.a++;
        log.info("{}", test.a);
        test.e.a = test.a * 2;
    }

}
