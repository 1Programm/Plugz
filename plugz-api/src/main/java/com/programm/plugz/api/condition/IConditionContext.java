package com.programm.plugz.api.condition;

import com.programm.plugz.api.ISubsystem;

import java.net.URL;
import java.util.List;

public interface IConditionContext {

    String getConfig(String name);

    boolean hasInstanceOfType(Class<?> type);

    List<URL> scanUrls();

    List<Class<?>> subsystems();

}
