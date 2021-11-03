package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.PostSetup;
import com.programm.projects.plugz.magic.PreSetup;
import com.programm.projects.plugz.magic.Service;

@Service
public class AService {

    @PreSetup
    public void preInit(){
        System.out.println("Before Init!");
    }

    @PostSetup
    public void init(){
        System.out.println("After Init!");
    }

}
