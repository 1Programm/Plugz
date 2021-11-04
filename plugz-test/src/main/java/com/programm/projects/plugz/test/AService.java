package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.Get;
import com.programm.projects.plugz.magic.PostSetup;
import com.programm.projects.plugz.magic.PreSetup;
import com.programm.projects.plugz.magic.Service;

@Service
public class AService {

//    private final PrinterService printer;
//
//    public AService(@Get PrinterService printer){
//        this.printer = printer;
//    }

    @PreSetup
    public void preInit(@Get PrinterService printer){
//        printer.print("Before Init!");
        System.out.println("Before Init!");
    }

    @PostSetup
    public void init(){
//        printer.print("After Init!");
        System.out.println("After Init!");
    }

}
