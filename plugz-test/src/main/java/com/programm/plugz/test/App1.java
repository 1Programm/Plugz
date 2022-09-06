package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.Config;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.condition.Conditional;
import com.programm.plugz.api.lifecycle.PostSetup;

@Config
@Conditional("${hello}")
public class App1 {

    @Get private ILogger log;

    @PostSetup
    public void setupConfigs(){
        log.info("%30|[#]( [APP 1] )");
    }

}
