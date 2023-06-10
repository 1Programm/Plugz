package com.programm.plugz.persist;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.*;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.instance.IInstanceManager;
import com.programm.plugz.cls.analyzer.*;
import com.programm.plugz.persist.ex.PersistException;
import com.programm.plugz.persist.ex.PersistQueryBuildException;
import com.programm.plugz.persist.ex.PersistShutdownException;
import com.programm.plugz.persist.ex.PersistStartupException;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.*;

@Logger("Persist")
public class PersistSubsystem implements ISubsystem {

    private final ILogger log;
    private final IInstanceManager instanceManager;

    private final ClassAnalyzer analyzer = new ClassAnalyzer(true, true, true);
    private final List<Class<?>> entityClasses = new ArrayList<>();
    private final List<Class<?>> repositoryClasses = new ArrayList<>();
    private final Map<Class<?>, PersistEntityInfo> entityInfoMap = new HashMap<>();
    private final Map<Class<?>, Class<?>> repoClsToEntityClsMap = new HashMap<>();

    private IRepoHandler repoHandler;

    public PersistSubsystem(@Get ILogger log, @Get PlugzConfig config, @Get IInstanceManager instanceManager) {
        this.log = log;
        this.instanceManager = instanceManager;
    }

    @Override
    public void registerSetup(ISubsystemSetupHelper setupHelper, AnnotationChecker annocheck) throws MagicException {
        setupHelper.registerSearchClass(IRepoHandler.class, this::onClassImplementingPersistRepoHandler);
        setupHelper.registerClassAnnotation(Entity.class, this::onClassAnnotatedWithEntity);
        setupHelper.registerClassAnnotation(Repo.class, this::onClassAnnotatedWithRepo);

        annocheck.forClass(CustomQuery.class, ElementType.METHOD)
                        .classAnnotations().whitelist().and(Repo.class).seal();

        annocheck.forClass(Repo.class, ElementType.TYPE)
                .partnerAnnotations().blacklist().and(Entity.class).seal();

        annocheck.forClass(ID.class, ElementType.FIELD)
                .classAnnotations().whitelist().and(Entity.class).seal();

        annocheck.forClass(ID.class, ElementType.FIELD)
                .partnerAnnotations().blacklist().and(ForeignKey.class).seal();

        annocheck.forClass(ForeignKey.class, ElementType.FIELD)
                .classAnnotations().whitelist().and(Entity.class).seal();

        annocheck.forClass(ForeignKey.class, ElementType.FIELD)
                .partnerAnnotations().blacklist().and(Generated.class).seal();

        annocheck.forClass(Generated.class, ElementType.FIELD)
                .classAnnotations().whitelist().and(Entity.class).seal();
    }

    @Override
    public void startup() throws MagicException {
        if(repoHandler == null){
            log.error("No {} implementation found! Persist will not work!", IRepoHandler.class.getSimpleName());
            return;
        }

        for(Class<?> entityClass : entityClasses) {
            PersistEntityInfo entityInfo = collectEntityInfo(entityClass);
            entityInfoMap.put(entityClass, entityInfo);
        }

        log.info("Starting up RepoHandler: [{}].", repoHandler.getClass().getName());
        try {
            repoHandler.startup(analyzer, entityInfoMap);
        }
        catch (PersistStartupException e){
            throw new MagicSetupException(e);
        }

        for(Class<?> repoClass : repositoryClasses){
            Class<?> entityClass = repoClsToEntityClsMap.get(repoClass);
            PersistEntityInfo entityInfo = entityInfoMap.get(entityClass);

            if(entityInfo == null) throw new MagicSetupException("Repository [" + repoClass + "] does not map to a valid, registered entity class [" + entityClass + "]!");

            try {
                Object repoImplementation = repoHandler.createRepoImplementation(repoClass, entityInfo, entityInfoMap);
                instanceManager.registerInstance(repoClass, repoImplementation);
            }
            catch (PersistQueryBuildException e){
                throw new MagicSetupException("Failed register repository [" + repoClass + "].", e);
            }
        }
    }

    private PersistEntityInfo collectEntityInfo(Class<?> cls) throws MagicException {
        AnalyzedPropertyClass analyzedEntityClass;
        try {
            analyzedEntityClass = analyzer.analyzeProperty(cls);
        }
        catch (ClassAnalyzeException e){
            throw new MagicSetupException("Failed to analyze entity [" + cls.getName() + "].", e);
        }

        try {
            List<String> primaryKeys = new ArrayList<>();
            Map<String, PersistForeignKeyInfo> secondaryKeys = new HashMap<>();
            collectKeyInfo(analyzedEntityClass, primaryKeys, secondaryKeys);
            if(primaryKeys.size() == 0) throw new PersistStartupException("No primary key!");
            if(primaryKeys.size() > 1) throw new PersistStartupException("Multiple primary keys!");

            String primaryKey = primaryKeys.get(0);

            return new PersistEntityInfo(cls, analyzedEntityClass, primaryKey, secondaryKeys);
        }
        catch (PersistStartupException e){
            throw new MagicSetupException("Failed to prepare entity [" + cls + "].", e);
        }
    }

    private void collectKeyInfo(AnalyzedPropertyClass cls, List<String> primaryKeys, Map<String, PersistForeignKeyInfo> secondaryKeys) throws PersistStartupException {
        Map<String, PropertyEntry> fieldEntries = cls.getFieldEntryMap();

        for(String fieldName : fieldEntries.keySet()){
            PropertyEntry fieldEntry = fieldEntries.get(fieldName);
            Field field = fieldEntry.getField();
            if(field.isAnnotationPresent(ID.class)){
                primaryKeys.add(fieldName);
            }

            ForeignKey foreignKeyAnnotation = field.getAnnotation(ForeignKey.class);
            if(foreignKeyAnnotation != null){
                String foreignKeyName = foreignKeyAnnotation.value();
                AnalyzedPropertyClass otherEntityType = fieldEntry.getPropertyType();
                boolean manyToConnection = false;

                //1. Iterable   -> Many-To-? connection
                //2. Else       -> One-To-? connection
                try {
                    AnalyzedPropertyClass otherEntityTypeInsideIterable = getFieldClassFromPossibleIterable(fieldEntry);
                    if(otherEntityTypeInsideIterable != null){
                        manyToConnection = true;
                        otherEntityType = otherEntityTypeInsideIterable;
                    }
                }
                catch (ClassAnalyzeException e){
                    throw new PersistStartupException("Failed to analyze type T of iterable property: [" + fieldName + "]!", e);
                }
                catch (PersistException e){
                    throw new PersistStartupException("Failed to get possible entity type from iterable!");
                }

                if(!entityInfoMap.containsKey(otherEntityType.getType())){
                    throw new PersistStartupException("Foreign key does not annotate a defined entity class!");
                }


                Map<String, PropertyEntry> oFieldEntries = otherEntityType.getFieldEntryMap();

                PropertyEntry uniqueForeignKeyEntry = oFieldEntries.get(foreignKeyName);
                if(uniqueForeignKeyEntry == null) throw new PersistStartupException("Foreign Entity [" + otherEntityType.getType() + "] does not have a member [" + foreignKeyName + "] to map to!");
                Field uniqueForeignKeyEntryField = uniqueForeignKeyEntry.getField();
                if(!uniqueForeignKeyEntryField.isAnnotationPresent(ID.class)) throw new PersistStartupException("Foreign Entity [" + otherEntityType.getType() + "] - member [" + foreignKeyName + "] is not a unique key annotated by @ID!");



                PersistForeignKeyInfo.ConnectionType connectionType = null;

                //Go through all @ForeignKey fields
                for(String oFieldName : oFieldEntries.keySet()) {
                    PropertyEntry oFieldEntry = oFieldEntries.get(oFieldName);
                    Field oField = oFieldEntry.getField();

                    if(oField.isAnnotationPresent(ForeignKey.class)){
                        boolean toManyConnection = false;
                        AnalyzedPropertyClass oOtherEntityType = oFieldEntry.getPropertyType();
                        try {
                            AnalyzedPropertyClass otherEntityTypeInsideIterable = getFieldClassFromPossibleIterable(fieldEntry);
                            if(otherEntityTypeInsideIterable != null){
                                toManyConnection = true;
                                oOtherEntityType = otherEntityTypeInsideIterable;
                            }
                        }
                        catch (ClassAnalyzeException e){
                            throw new PersistStartupException("Failed to analyze type T of iterable property: [" + fieldName + "]!", e);
                        }
                        catch (PersistException e){
                            throw new PersistStartupException("Failed to get possible entity type from iterable!");
                        }

                        //We found matching pair
                        if(oOtherEntityType.getType() == cls.getType()){
                            connectionType = manyToConnection ? (toManyConnection ? PersistForeignKeyInfo.ConnectionType.MANY_TO_MANY : PersistForeignKeyInfo.ConnectionType.MANY_TO_ONE) : (toManyConnection ? PersistForeignKeyInfo.ConnectionType.ONE_TO_MANY : PersistForeignKeyInfo.ConnectionType.ONE_TO_ONE);
                            break;
                        }
                    }
                }

                if(connectionType == null){
                    connectionType = manyToConnection ? PersistForeignKeyInfo.ConnectionType.MANY_TO_ONE : PersistForeignKeyInfo.ConnectionType.ONE_TO_ONE;
                }


                PersistForeignKeyInfo foreignKeyInfo = new PersistForeignKeyInfo(connectionType, otherEntityType.getType(), foreignKeyName);
                secondaryKeys.put(fieldName, foreignKeyInfo);
            }
        }
    }

    private AnalyzedPropertyClass getFieldClassFromPossibleIterable(PropertyEntry fieldEntry) throws ClassAnalyzeException, PersistException {
        AnalyzedPropertyClass cls = fieldEntry.getPropertyType();
        Class<?> type = cls.getType();
        if(Iterable.class.isAssignableFrom(type)){
            String typeName;
            if(List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)){
                typeName = "E";
            }
            else {
                typeName = "T";
            }

            AnalyzedParameterizedType iteratorParamType = fieldEntry.getParameterizedType(typeName);
            if(iteratorParamType == null) throw new PersistException("Unexpected type name: [" + typeName + "] for property entry: " + fieldEntry);
            return analyzer.analyzeProperty(iteratorParamType, iteratorParamType.getType());
        }

        return null;
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

    private void onClassImplementingPersistRepoHandler(Class<?> cls, IInstanceManager manager) throws MagicInstanceException {
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
        entityInfoMap.put(cls, null);
    }

    private void onClassAnnotatedWithRepo(Repo annotation, Class<?> cls, IInstanceManager manager) {
        Class<?> entityCls = annotation.value();
        log.debug("Found repo: [{}] for entity [{}].", cls.getName(), entityCls.getName());
        repositoryClasses.add(cls);
        repoClsToEntityClsMap.put(cls, annotation.value());
    }

}
