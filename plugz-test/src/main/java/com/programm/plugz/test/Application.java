package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.Service;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.lifecycle.PostStartup;
import com.programm.plugz.magic.MagicEnvironment;

@Service
public class Application {

    public static void main(String[] args) throws Exception {
        MagicEnvironment.Start(args);
    }

    @Get private ILogger log;

    @PostStartup
    public void setup(@Get String abc){
        log.info("> Repo: {}", "Test", abc);
    }


}
