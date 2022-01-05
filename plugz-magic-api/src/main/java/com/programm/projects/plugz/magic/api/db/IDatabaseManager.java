package com.programm.projects.plugz.magic.api.db;

import com.programm.projects.plugz.magic.api.ISubsystem;

import java.net.URL;

public interface IDatabaseManager extends ISubsystem {

    void removeUrl(URL url);

    void registerEntity(Class<?> cls) throws DataBaseException;

    Object registerAndImplementRepo(Class<?> cls) throws DataBaseException;

}
