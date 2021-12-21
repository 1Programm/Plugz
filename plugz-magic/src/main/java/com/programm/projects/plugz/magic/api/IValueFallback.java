package com.programm.projects.plugz.magic.api;

public interface IValueFallback {

    Object fallback(Class<?> expectedType, String resourceName, Object source);

}
