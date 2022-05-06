package com.programm.plugz.test;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.webserv.api.GetMapping;
import com.programm.plugz.webserv.api.RequestParam;
import com.programm.plugz.webserv.api.RestController;

import java.util.Collections;
import java.util.List;

@RestController("/search")
public class SearchController {

    @Get private ILogger log;

    @GetMapping("/simple")
    public List<SimpleData> doSimpleSearch(@RequestParam("query") String query){
        log.info("Query: {}", query);
        return Collections.emptyList();
    }

}
