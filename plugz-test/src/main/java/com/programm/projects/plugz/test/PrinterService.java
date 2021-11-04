package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.Get;
import com.programm.projects.plugz.magic.PostSetup;
import com.programm.projects.plugz.magic.Service;

@Service
public class PrinterService {

//    @Get
//    private AService service;

    @PostSetup
    public void test(@Get AService a){
        print("Hey");
        a.preInit(this);
    }

    public void print(String txt){
        System.out.println("PRINT: " + txt);
    }

}
