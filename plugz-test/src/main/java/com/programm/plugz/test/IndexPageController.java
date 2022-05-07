package com.programm.plugz.test;

import com.programm.plugz.webserv.api.GetMapping;
import com.programm.plugz.webserv.api.RestController;

@RestController("/index")
public class IndexPageController {

    @GetMapping(contentType = "text/html")
    public String getIndexView(){
        return "<a href=\"/goto-other\">This is [8080]! Click me :D</a>";
    }

}
