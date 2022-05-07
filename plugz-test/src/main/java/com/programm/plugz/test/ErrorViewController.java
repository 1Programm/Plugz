package com.programm.plugz.test;

import com.programm.plugz.webserv.api.GetMapping;
import com.programm.plugz.webserv.api.RestController;

@RestController(value = "/error", defaultContentType = "text/html")
public class ErrorViewController {

    @GetMapping("/unauthorized")
    public String getUnauthorizedView(){
        return "<h1 style=\"color: red;\">Unauthorized Access</h1>";
    }

}
