package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.webserv.api.GetMapping;
import com.programm.plugz.webserv.api.RestController;

@RestController
public class TestController {

    @Get private ILogger log;

    @GetMapping("/test")
    public void test(){
        log.info("Test");
    }

}
