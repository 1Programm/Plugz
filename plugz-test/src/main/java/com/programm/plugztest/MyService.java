package com.programm.plugztest;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.Service;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.debugger.DValue;
import com.programm.plugz.debugger.DebugValue;
import com.programm.plugz.schedules.Scheduled;

@Service
public class MyService {

    @Get private ILogger log;

    @DebugValue
    private DValue.Int num = new DValue.Int();

    @Scheduled(repeat = 1000, stopAfter = 5000)
    public void test(){
        log.info("{}", num);
        num.increment();
    }

}
