package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.Config;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.auto.GetConfig;
import com.programm.plugz.api.lifecycle.PostSetup;
import com.programm.plugz.webserv.api.config.RestConfig;

@Config("app1")
public class App1 {

    @Get private ILogger log;

    @GetConfig("frontend-url")
    private String frontendUrl;

    @PostSetup
    public void setupConfigs(@Get RestConfig config){
        log.info("%30|[#](APP 1)");
        config.fallbackInterceptor(new DefaultFallbackWebsite());

        config.forPaths("/")
                .onSuccess(new SimpleRedirectInterceptor("http://localhost:8080", "/home"));

        config.forPaths("/bla/home")
                .onSuccess((handler, request) -> request.doOk("text/html", "<h1>A Text</h1>"));

        config.forPaths("/home")
                .onSuccess(new PassRequestInterceptor("http://localhost:8080/bla"));
    }

}
