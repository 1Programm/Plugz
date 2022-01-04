package com.programm.projects.plugz.magic.api;

public interface ISubsystem {

    void startup() throws MagicException;
    void shutdown() throws MagicException;

}
