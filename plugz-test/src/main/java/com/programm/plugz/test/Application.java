package com.programm.plugz.test;

import com.programm.plugz.magic.MagicEnvironment;
import com.programm.plugz.webserv.api.PostMapping;
import com.programm.plugz.webserv.api.RequestBody;
import com.programm.plugz.webserv.api.RestController;

@RestController
public class Application {

    public static void main(String[] args) throws Exception {
        MagicEnvironment.Start(args);
    }

    @PostMapping("/person/new")
    public void createPerson(@RequestBody Person person){
        System.out.println("P");
    }

}
