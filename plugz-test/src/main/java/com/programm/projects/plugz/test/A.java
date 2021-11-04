package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.Get;
import com.programm.projects.plugz.magic.PostSetup;
import com.programm.projects.plugz.magic.Service;

@Service
public class A {

    @PostSetup
    public void init(@Get IPrinter printer){
        printer.print("HELLOOO");
    }

}
