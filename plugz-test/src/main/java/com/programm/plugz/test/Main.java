package com.programm.plugz.test;

import com.programm.plugz.api.Config;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.lifecycle.PreSetup;
import com.programm.plugz.magic.MagicEnvironment;

@Config
public class Main {

    public static void main(String[] args) throws Exception {
        MagicEnvironment.Start(args);
    }


    @Get
    public void setup(String s){
        System.out.println("HI");
    }

}
