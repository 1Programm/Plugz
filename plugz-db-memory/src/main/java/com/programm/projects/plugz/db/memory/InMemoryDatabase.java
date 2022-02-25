package com.programm.projects.plugz.db.memory;

import com.programm.projects.ioutils.log.api.out.ILogger;
import com.programm.projects.plugz.db.abstractbase.entity.EntityEntry;
import com.programm.projects.plugz.db.abstractbase.entity.EntityMapException;
import com.programm.projects.plugz.db.abstractbase.entity.EntityMapper;
import com.programm.projects.plugz.db.abstractbase.repo.*;
import com.programm.projects.plugz.magic.api.Get;
import com.programm.projects.plugz.magic.api.db.*;

import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

public class InMemoryDatabase implements IDatabaseManager, IQueryExecutor {

    @Get private ILogger log;

    private final Map<Class<?>, EntityEntry> entityMap = new HashMap<>();
    private final Map<Class<?>, IDatabase<Object, Object>> databaseMap = new HashMap<>();

    @Override
    public void startup() {
        log.info("In memory database. All data will be lost on exit!");
    }

    @Override
    public void shutdown() {}

    @Override
    public void removeUrl(URL url) {
        //TODO removing urls...
    }

    @Override
    public void registerEntity(Class<?> cls) throws DataBaseException{
        if(!entityMap.containsKey(cls)){
            log.debug("Register Entity: {}", cls);

            try {
                EntityEntry entityEntry = EntityMapper.createEntry(cls);
                entityMap.put(cls, entityEntry);

                IDatabase<Object, Object> db = databaseMap.get(cls);
                if(db == null){
                    db = generateDatabase(entityEntry);
                    databaseMap.put(cls, db);
                }
            }
            catch (EntityMapException e){
                throw new DataBaseException("Exception inspecting the @Entity [" + cls.getName() + "]", e);
            }
            catch (DataBaseException e){
                throw new DataBaseException("", e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private IDatabase<Object, Object> generateDatabase(EntityEntry entityEntry) throws DataBaseException {
        Class<?> idCls = entityEntry.getIdCls();

        if(idCls == Integer.class){
            return (NumIdDatabase) new NumIdDatabase.IntDatabase<>(entityEntry);
        }
        else if(idCls == Long.class){
            return (NumIdDatabase) new NumIdDatabase.LongDatabase<>(entityEntry);
        }
        else {
            throw new DataBaseException("No database generator found for id-type: [" + idCls.getName() + "]!");
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

        EntityEntry entityEntry = entityMap.get(dataCls);

        RepoEntry repoEntry;
        try {
            repoEntry = RepoMapper.createEntry(cls, idCls, entityEntry);
        }
        catch (DataBaseException e){
            throw new DataBaseException("Exception inspecting the @Repo [" + cls.getName() + "]", e);
        }

        InvocationHandler handler = new RepoProxyHandler(repoEntry, this);
        return cls.cast(Proxy.newProxyInstance(cls.getClassLoader(), new Class<?>[]{cls}, handler));
    }

    @Override
    public Object execute(String query) throws QueryExecuteException {
        log.info("EXECUTE: {}", query);
        String[] split = query.split(" ");

        if(query.equals("count all")){

        }

        return null;
    }
}
