package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.Config;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.lifecycle.PostSetup;
import com.programm.plugz.magic.MagicEnvironment;
import com.programm.plugz.webserv.api.config.RestConfig;

@Config
public class Main {

    public static void main(String[] args) throws Exception {
        MagicEnvironment.Start(args);
    }

    @Get private ILogger log;

    @PostSetup
    public void configureRestConfig(@Get RestConfig config){
        config.forPath("/test")
                .with(new TestRequestValidator(false))
                .onUnauthorizedAccessRedirect("/error/unauthorized")
                .onSuccessContinue();
    }

}
