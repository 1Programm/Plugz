package com.programm.projects.plugz.magic.api.db;

import java.net.URL;

public interface IDatabaseManager {

    void startup() throws DataBaseException;

    void shutdown() throws DataBaseException;

    void removeUrl(URL url);

    void registerEntity(Class<?> cls);

    Object registerAndImplementRepo(Class<?> cls) throws DataBaseException;

}
