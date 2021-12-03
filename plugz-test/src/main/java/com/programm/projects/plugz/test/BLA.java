package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.api.Get;
import com.programm.projects.plugz.magic.api.Service;
import com.programm.projects.plugz.magic.api.ISchedules;

@Service
public class BLA {

    public void test(String name, @Get ISchedules schedules){
        System.out.println("NAME: [" + name + "]: " + schedules);
    }

}
