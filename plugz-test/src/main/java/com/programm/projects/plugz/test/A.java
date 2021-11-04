package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.Get;
import com.programm.projects.plugz.magic.PostSetup;
import com.programm.projects.plugz.magic.Service;

@Service
public class A {

    private final B b;

    public A(@Get B b){
        this.b = b;
    }

    @PostSetup
    public void test(){
        System.out.println("HELLOOO");
    }

}
