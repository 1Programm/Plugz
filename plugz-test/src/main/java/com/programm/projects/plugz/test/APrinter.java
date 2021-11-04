package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.Service;

@Service
public class APrinter implements IPrinter {

    @Override
    public void print(String t) {
        System.out.println("Test");
    }
}
