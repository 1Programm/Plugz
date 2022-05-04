package com.programm.plugz.test;

import com.programm.plugz.api.Service;

@Service
public class UserService {

    public User getDefaultUser(){
        User u1 = new User();
        u1.name = "Julian";
        u1.age = 22;
        u1.parent = new User();
        u1.parent.name = "Gerni";

        return u1;
    }

}
