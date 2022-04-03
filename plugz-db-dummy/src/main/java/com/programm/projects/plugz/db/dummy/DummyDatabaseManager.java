package com.programm.projects.plugz.db.dummy;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.ioutils.log.api.out.Logger;
import com.programm.projects.plugz.db.abstractbase.entity.EntityEntry;
import com.programm.projects.plugz.db.abstractbase.entity.EntityMapException;
import com.programm.projects.plugz.db.abstractbase.entity.EntityMapper;
import com.programm.projects.plugz.db.abstractbase.repo.*;
import com.programm.projects.plugz.magic.api.Get;
import com.programm.projects.plugz.magic.api.MagicException;
import com.programm.projects.plugz.magic.api.db.DataBaseException;
import com.programm.projects.plugz.magic.api.db.IDatabaseManager;
import com.programm.projects.plugz.magic.api.db.IRepo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Logger("Dummy DB")
public class DummyDatabaseManager implements IDatabaseManager {

    @Get private ILogger log;

    private final Map<Class<?>, EntityEntry> entityEntries = new HashMap<>();
    private final Map<Class<?>, Dummybase> dbs = new HashMap<>();

    @Override
    public void startup() throws MagicException {
        log.info("Database started.");
    }

    @Override
    public void shutdown() throws MagicException {
        log.info("Database stopped.");
    }

    @Override
    public void removeUrl(URL url) {

    }

    @Override
    public void registerEntity(Class<?> cls) throws DataBaseException {
        try {
            EntityEntry entityEntry = EntityMapper.createEntry(cls);
            entityEntries.put(cls, entityEntry);
        }
        catch (EntityMapException e){
            throw new DataBaseException("Failed to create entity entry from class: [" + cls.getName() + "]", e);
        }
    }

    @Override
    public Object registerAndImplementRepo(Class<?> cls) throws DataBaseException {
        if(!IRepo.class.isAssignableFrom(cls)){
            throw new DataBaseException("Must implement the IRepo interface!");
        }

        Type[] genericTypes = cls.getGenericInterfaces();
        ParameterizedType paramTypes = (ParameterizedType)genericTypes[0];

        Type[] actualGenericTypes = paramTypes.getActualTypeArguments();
        Type idType = actualGenericTypes[0];
        Type dataType = actualGenericTypes[1];
        Class<?> idCls = (Class<?>)idType;
        Class<?> dataCls = (Class<?>)dataType;

        EntityEntry entityEntry = entityEntries.get(dataCls);
        if(entityEntry == null) throw new DataBaseException("No @Entity registered for type: [" + dataCls.getName() + "]!");

        RepoEntry repoEntry;
        try {
            repoEntry = RepoMapper.createEntry(cls, idCls, entityEntry);
        }
        catch (DataBaseException e){
            throw new DataBaseException("Failed to create repo entry from class: [" + cls.getName() + "]", e);
        }

        Dummybase dummybase = new Dummybase(entityEntry);
        dummybase.setup();
        dbs.put(dataCls, dummybase);

        InvocationHandler handler = new RepoProxyHandler(repoEntry, dummybase);
        return cls.cast(Proxy.newProxyInstance(cls.getClassLoader(), new Class<?>[]{cls}, handler));
    }
}
