package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.api.IValueFallback;

public class TestFallback implements IValueFallback {

    @Override
    public Object fallback(Class<?> expectedType, String resourceName, Object source) {
        return 99;
    }
}
