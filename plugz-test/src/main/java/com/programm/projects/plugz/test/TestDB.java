package com.programm.projects.plugz.test;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.plugz.magic.api.Get;
import com.programm.projects.plugz.magic.api.db.DataBaseException;
import com.programm.projects.plugz.magic.api.db.IDatabaseManager;

import java.net.URL;

public class TestDB implements IDatabaseManager {

    @Get private ILogger log;

    @Override
    public void startup() throws DataBaseException {
        log.info("DB startup");
    }

    @Override
    public void shutdown() throws DataBaseException {
        log.info("DB shutdown");
    }

    @Override
    public void removeUrl(URL url) {

    }

    @Override
    public void registerEntity(Class<?> cls) {
        log.info("Register: [{}]", cls);
    }

    @Override
    public Object registerAndImplementRepo(Class<?> cls) throws DataBaseException {
        log.info("Implement: [{}].", cls);
        return (UserRepo) TestUser::new;
    }
}
