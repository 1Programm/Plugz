package com.programm.plugz.persist;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.instance.IInstanceManager;
import com.programm.plugz.cls.analyzer.AnalyzedPropertyClass;
import com.programm.plugz.cls.analyzer.ClassAnalyzeException;
import com.programm.plugz.cls.analyzer.ClassAnalyzer;
import com.programm.plugz.cls.analyzer.PropertyEntry;
import com.programm.plugz.persist.ex.PersistException;
import com.programm.plugz.persist.ex.PersistQueryBuildException;
import com.programm.plugz.persist.ex.PersistShutdownException;
import com.programm.plugz.persist.ex.PersistStartupException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Logger("Persist")
public class PersistSubsystem implements ISubsystem {

    private final ILogger log;
    private final IInstanceManager instanceManager;

    private final ClassAnalyzer analyzer = new ClassAnalyzer(true, false, false, false);
    private final List<Class<?>> entityClasses = new ArrayList<>();
    private final Map<Class<?>, Class<?>> entityClsToRepositoryClsMap = new HashMap<>();

    private IRepoHandler repoHandler;

    public PersistSubsystem(@Get ILogger log, @Get PlugzConfig config, @Get IInstanceManager instanceManager) {
        this.log = log;
        this.instanceManager = instanceManager;
    }

    @Override
    public void registerSetup(ISubsystemSetupHelper setupHelper, AnnotationChecker annocheck) throws MagicException {
        setupHelper.registerSearchClass(IRepoHandler.class, this::onClassImplementingPersistRepoFactory);
        setupHelper.registerClassAnnotation(Entity.class, this::onClassAnnotatedWithEntity);
        setupHelper.registerClassAnnotation(Repo.class, this::onClassAnnotatedWithRepo);
    }

    @Override
    public void startup() throws MagicException {
        if(repoHandler == null){
            log.error("No {} implementation found! Persist will not work!", IRepoHandler.class.getSimpleName());
            return;
        }

        log.info("Starting up RepoHandler: [{}].", repoHandler.getClass().getName());
        try {
            repoHandler.startup(analyzer);
        }
        catch (PersistStartupException e){
            throw new MagicSetupException(e);
        }

        for(Map.Entry<Class<?>, Class<?>> entry : entityClsToRepositoryClsMap.entrySet()){
            Class<?> entityCls = entry.getKey();
            Class<?> repoCls = entry.getValue();

            if(!entityClasses.contains(entityCls)) throw new MagicSetupException("Class [" + entityCls.getName() + "] is not an entity!");

            try {
                AnalyzedPropertyClass analyzedEntityClass = analyzer.analyzeProperty(entityCls);
                checkValidEntity(analyzedEntityClass);

                Object repoImplementation = repoHandler.createRepoImplementation(repoCls, analyzedEntityClass);
                instanceManager.registerInstance(repoCls, repoImplementation);
            }
            catch (ClassAnalyzeException e){
                throw new MagicSetupException("Failed to analyze entity [" + entityCls.getName() + "].", e);
            }
            catch (PersistQueryBuildException e){
                throw new MagicSetupException("Failed to parse queries for repository [" + repoCls + "].", e);
            }
        }
    }

    private void checkValidEntity(AnalyzedPropertyClass analyzedEntityClass) throws MagicException {
        try {
            Map<String, PropertyEntry> fieldEntries = analyzedEntityClass.getFieldEntryMap();

            //Check for an field with an @ID annotation
            boolean hasPrimaryKey = false;

            for (String fieldName : fieldEntries.keySet()) {
                PropertyEntry fieldEntry = fieldEntries.get(fieldName);

                if(fieldEntry.getField().isAnnotationPresent(ID.class)) {
                    if (hasPrimaryKey) throw new PersistException("Multiple primary keys!");
                    hasPrimaryKey = true;
                }
            }

            if(!hasPrimaryKey) throw new PersistException("No primary key!");
        }
        catch (PersistException e){
            throw new MagicSetupException("Invalid Entity class [" + analyzedEntityClass.getType() + "]", e);
        }
    }

    @Override
    public void shutdown() throws MagicException {
        try {
            repoHandler.shutdown();
        }
        catch (PersistShutdownException e){
            throw new MagicException(e);
        }
    }

    private void onClassImplementingPersistRepoFactory(Class<?> cls, IInstanceManager manager) throws MagicInstanceException {
        log.debug("Found {} implementation: [{}]", IRepoHandler.class.getSimpleName(), cls.getName());
        if(repoHandler != null) {
            log.warn("Multiple {} implementations found!", IRepoHandler.class.getSimpleName());
            return;
        }

        manager.instantiate(cls, handlerInstance -> repoHandler = (IRepoHandler) handlerInstance);
    }

    private void onClassAnnotatedWithEntity(Entity annotation, Class<?> cls, IInstanceManager manager) {
        log.debug("Found entity: [{}].", cls.getName());
        entityClasses.add(cls);
    }

    private void onClassAnnotatedWithRepo(Repo annotation, Class<?> cls, IInstanceManager manager) {
        Class<?> entityCls = annotation.value();
        log.debug("Found repo: [{}] for entity [{}].", cls.getName(), entityCls.getName());
        entityClsToRepositoryClsMap.put(annotation.value(), cls);
    }

}
