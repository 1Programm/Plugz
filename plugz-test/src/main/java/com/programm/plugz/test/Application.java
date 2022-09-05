package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.Config;
import com.programm.plugz.api.auto.AutoWaitType;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.auto.GetConfig;
import com.programm.plugz.api.auto.Set;
import com.programm.plugz.api.lifecycle.PostSetup;
import com.programm.plugz.magic.MagicEnvironment;
import com.programm.plugz.webserv.api.config.RestConfig;

@Config
public class Application {

    public static void main(String[] args) throws Exception {
        MagicEnvironment.Start(args);
    }

    @GetConfig("frontend-url")
    private String frontendUrl;

    @PostSetup
    public void setupConfigs(@Get RestConfig config){
        config.fallbackInterceptor(new DefaultFallbackWebsite());

        config.forPaths("/")
                .onSuccess(new SimpleRedirectInterceptor("http://localhost:8080", "/home"));

        config.forPaths("/home")
                .onSuccess(new PassRequestInterceptor(frontendUrl));
    }

}
