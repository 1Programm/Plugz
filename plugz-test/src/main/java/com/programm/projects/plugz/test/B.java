package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.Get;
import com.programm.projects.plugz.magic.Service;

@Service
public class B {

    private final A a;

    public B(@Get A a){
        this.a = a;
    }

}
