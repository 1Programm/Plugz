package com.programm.projects.plugz.magic.api.debug;

import com.programm.projects.plugz.magic.api.ISubsystem;

import java.lang.reflect.Field;

public interface IDebugManager extends ISubsystem {

    void registerDebugValue(Object instance, Field field, Debug debugAnnotation) throws MagicDebugSetupException;

}
