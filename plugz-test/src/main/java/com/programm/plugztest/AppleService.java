package com.programm.plugztest;

import com.programm.plugz.api.Service;
import com.programm.plugz.api.auto.Set;

@Service
public class AppleService {

    private static final String[] APPLE_NAMES = { "Pink Lady", "Braeburn", "bla", "green apple", "London" };

    @Set
    public String getApple(){
        return APPLE_NAMES[(int)(Math.random() * APPLE_NAMES.length)];
    }

}
