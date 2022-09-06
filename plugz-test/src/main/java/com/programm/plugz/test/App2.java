package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.Config;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.condition.Conditional;
import com.programm.plugz.api.lifecycle.PostSetup;

@Config
public class App2 {

    @Get private ILogger log;

    @PostSetup
    @Conditional("${hello} == true")
    public void onSetup(){
        log.info("%30|[#]( [APP 2] )");
    }

}
