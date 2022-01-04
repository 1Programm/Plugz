package com.programm.projects.plugz.magic.api.resources;

public interface IValueFallback {

    Object fallback(Class<?> expectedType, String resourceName, Object source);

}
