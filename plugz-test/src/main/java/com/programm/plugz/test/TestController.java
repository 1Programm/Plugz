package com.programm.plugz.test;

import com.programm.plugz.api.auto.Get;
import com.programm.plugz.webserv.api.*;

@RestController
public class TestController {

    @Get private UserService userService;

    @GetMapping("/test")
    public User hello0(){
        return userService.getDefaultUser();
    }

    @PutMapping
    public void hello1(){

    }

    @PostMapping
    public void hello2(){

    }

    @DeleteMapping
    public void hello3(){

    }

}
