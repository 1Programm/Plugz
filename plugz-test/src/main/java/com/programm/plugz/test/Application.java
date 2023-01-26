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
    @Get private PersonRepo repo;

    @PostStartup
    public void setup(){
        log.info("> Repo: {}", repo);
    }


}
