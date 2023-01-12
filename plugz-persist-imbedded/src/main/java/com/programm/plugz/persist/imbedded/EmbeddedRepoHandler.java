package com.programm.plugz.persist.imbedded;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.api.MagicInstanceException;
import com.programm.plugz.api.MagicRuntimeException;
import com.programm.plugz.api.PlugzConfig;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.instance.IInstanceManager;
import com.programm.plugz.cls.analyzer.*;
import com.programm.plugz.persist.Entity;
import com.programm.plugz.persist.IRepoHandler;
import com.programm.plugz.persist.ex.*;
import com.programm.plugz.persist.query.IParameterizedQuery;
import com.programm.plugz.persist.query.IQuery;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;

@Logger("Persist [Embedded]")
public class EmbeddedRepoHandler implements IRepoHandler {

    private static final AnalyzedParameterizedType TYPE_SET = new AnalyzedParameterizedType(Set.class, null, Collections.emptyMap());
    private static final AnalyzedParameterizedType TYPE_LIST = new AnalyzedParameterizedType(List.class, null, Collections.emptyMap());
    private static final AnalyzedParameterizedType TYPE_MAP = new AnalyzedParameterizedType(Map.class, null, Collections.emptyMap());
    private static final AnalyzedParameterizedType TYPE_ARRAY = new AnalyzedParameterizedType(Object[].class, null, Collections.emptyMap());

    private static final AnalyzedPropertyClass PROPERTY_SET = new AnalyzedPropertyClass(TYPE_SET, Collections.emptyMap(), HashSet::new);
    private static final AnalyzedPropertyClass PROPERTY_LIST = new AnalyzedPropertyClass(TYPE_LIST, Collections.emptyMap(), ArrayList::new);
    private static final AnalyzedPropertyClass PROPERTY_MAP = new AnalyzedPropertyClass(TYPE_MAP, Collections.emptyMap(), HashMap::new);
    private static final AnalyzedPropertyClass PROPERTY_ARRAY = new AnalyzedPropertyClass(TYPE_ARRAY, Collections.emptyMap(), null);

    private static String getUniqueMethodName(Method method){
        return method.toGenericString();
    }

    private static String getTableNameFromEntity(AnalyzedPropertyClass _entityCls) {
        Class<?> entityCls = _entityCls.getType();
        Entity entityAnnotation = entityCls.getAnnotation(Entity.class);
        if(entityAnnotation != null && !entityAnnotation.tableName().isEmpty()){
            return entityAnnotation.tableName().toUpperCase();
        }

        return entityCls.getSimpleName().toUpperCase();
    }

    @RequiredArgsConstructor
    private class EmbeddedInvocationHandler implements InvocationHandler {
        private final Map<String, MethodQueryInfo> methodQueryMap;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String mName = getUniqueMethodName(method);

            switch (mName){
                case "getClass":
                case "equals":
                case "toString":
                case "hashCode":
                case "notify":
                case "notifyAll":
                case "wait":
                    return method.invoke(proxy, args);
            }

            MethodQueryInfo info = methodQueryMap.get(mName);
            if(info == null) throw new PersistQueryExecuteException("INVALID STATE: No query defined for method [" + mName + "]!");

            return executeQuery(info.query, info.returnType, info.parameterTypes, args);
        }
    }


    private static final String TABLE_CREATE_MODE_CREATE = "create";
    private static final String TABLE_CREATE_MODE_OVERRIDE = "override";
    private static final String TABLE_CREATE_MODE_FAIL = "fail";





    private static final String CONF_PERSITS_DB_URL_NAME = "persist.db.url";
    private static final String CONF_PERSITS_DB_URL_DEFAULT = null;

    private static final String CONF_PERSITS_DB_USER_NAME = "persist.db.user";
    private static final String CONF_PERSITS_DB_USER_DEFAULT = "root";

    private static final String CONF_PERSITS_DB_PASSWORD_NAME = "persist.db.password";
    private static final String CONF_PERSITS_DB_PASSWORD_DEFAULT = "";

    private static final String CONF_PERSITS_DB_TABLE_CREATE_MODE_NAME = "persist.db.table_create_mode";
    private static final String CONF_PERSITS_DB_TABLE_CREATE_MODE_DEFAULT = TABLE_CREATE_MODE_FAIL;





    private final ILogger log;
    private final IInstanceManager instanceManager;

    private final String databaseUrl;
    private final String databaseUsername;
    private final String databasePassword;
    private final String tableCreateMode;

    private final Map<AnalyzedPropertyClass, DBEntityInfo> entityInfos = new HashMap<>();

    private ClassAnalyzer analyzer;
    private Connection databaseConnection;

    public EmbeddedRepoHandler(@Get ILogger log, @Get IInstanceManager instanceManager, @Get PlugzConfig config) {
        this.log = log;
        this.instanceManager = instanceManager;

        this.databaseUrl = config.getOrDefault(CONF_PERSITS_DB_URL_NAME, CONF_PERSITS_DB_URL_DEFAULT);
        this.databaseUsername = config.getOrDefault(CONF_PERSITS_DB_USER_NAME, CONF_PERSITS_DB_USER_DEFAULT);
        this.databasePassword = config.getOrDefault(CONF_PERSITS_DB_PASSWORD_NAME, CONF_PERSITS_DB_PASSWORD_DEFAULT);
        this.tableCreateMode = config.getOrDefault(CONF_PERSITS_DB_TABLE_CREATE_MODE_NAME, CONF_PERSITS_DB_TABLE_CREATE_MODE_DEFAULT);
    }

    private IClassPropertyBuilder provideBuilders(AnalyzedParameterizedType analyzedType){
        if(Set.class.isAssignableFrom(analyzedType.getType())){
            return HashSet::new;
        }
        else if(List.class.isAssignableFrom(analyzedType.getType())){
            return ArrayList::new;
        }
        else if(Map.class.isAssignableFrom(analyzedType.getType())) {
            return HashMap::new;
        }
        else if(analyzedType.getType().isArray()){
            return null;
        }

        return null;
    }

    @Override
    public void startup(ClassAnalyzer analyzer) throws PersistStartupException {
        this.analyzer = analyzer;
//        this.analyzer.registerBuilderProvider(this::provideBuilders);

        log.info("Starting up persist-connection.");
        log.debug("With arguments [url: '{}', user: '{}', pwd: '{}']", databaseUrl, databaseUsername, databasePassword);

        try {
            databaseConnection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
        }
        catch (SQLException e){
            throw new PersistStartupException("Failed to establish aqua-persist connection!", e);
        }

        try {
            instanceManager.registerInstance(Connection.class, databaseConnection);
        }
        catch (MagicInstanceException e){
            throw new PersistStartupException("Failed to register instance of db connection!", e);
        }
    }

    @Override
    public void shutdown() throws PersistShutdownException {
        log.debug("Shutting down current database connection...");
        try {
            databaseConnection.close();
        }
        catch (SQLException e){
            throw new PersistShutdownException("Failed to shut down current database connection.", e);
        }
    }

    @Override
    public Object createRepoImplementation(Class<?> cls, AnalyzedPropertyClass analyzedEntityClass) throws PersistQueryBuildException {
        log.debug("Creating Repository implementation for class [{}] with entity [{}] ...", cls, analyzedEntityClass.getType());
        String tableName = getTableNameFromEntity(analyzedEntityClass);
        log.trace("Table Name from entity: [{}]", tableName);

        DBEntityInfo entityInfo = entityInfos.get(analyzedEntityClass);
        if(entityInfo == null) {
            entityInfo = DBHelper.createDBEntityInfo(analyzedEntityClass);
            entityInfos.put(analyzedEntityClass, entityInfo);
        }

        try {
            checkCreateTable(tableName, analyzedEntityClass, entityInfo);
        }
        catch (PersistException e){
            throw new PersistQueryBuildException("Failed to create repo [" + cls + "]", e);
        }


        Map<String, MethodQueryInfo> methodQueryMap = new HashMap<>();
        Method[] declaredMethods = cls.getDeclaredMethods();
        for(Method method : declaredMethods){
            AnalyzedPropertyClass methodReturnType = analyzeMethodReturnType(method, analyzedEntityClass.getParameterizedTypeMap());

            String mName = getUniqueMethodName(method);
            MethodQueryInfo info = MethodQueryParser.parse(tableName, analyzedEntityClass, entityInfo, method, methodReturnType);
            methodQueryMap.put(mName, info);
        }

        EmbeddedInvocationHandler invocationHandler = new EmbeddedInvocationHandler(methodQueryMap);
        return Proxy.newProxyInstance(cls.getClassLoader(), new Class<?>[]{cls}, invocationHandler);
    }

    private void checkCreateTable(String tableName, AnalyzedPropertyClass analyzedEntityClass, DBEntityInfo entityInfo) throws PersistException {
        log.debug("Check-Create table [{}]...", tableName);
        if(tableCreateMode.equals(TABLE_CREATE_MODE_CREATE)){
            try {
                if(DBHelper.tableNExists(databaseConnection, tableName)) {
                    log.debug("Table does not exist!");
                    log.debug("Creating table [{}]...", tableName);
                    DBHelper.createTableForAnalyzedEntity(databaseConnection, tableName, analyzedEntityClass, entityInfo);
                }
            }
            catch (SQLException e){
                throw new PersistQueryBuildException("Failed to create missing table [" + tableName + "]!", e);
            }
        }
        else if(tableCreateMode.equals(TABLE_CREATE_MODE_OVERRIDE)){
            try {
                log.debug("Overriding table [{}]...", tableName);
                DBHelper.dropTable(databaseConnection, tableName);
                DBHelper.createTableForAnalyzedEntity(databaseConnection, tableName, analyzedEntityClass, entityInfo);
            }
            catch (SQLException e){
                throw new PersistQueryBuildException("Failed to override table [" + tableName + "]!", e);
            }
        }
        else if(tableCreateMode.equals(TABLE_CREATE_MODE_FAIL)){
            try {
                if(DBHelper.tableNExists(databaseConnection, tableName)) {
                    throw new PersistQueryBuildException("Table [" + tableName + "] does not exist while in fail mode!");
                }
            }
            catch (SQLException e){
                throw new PersistQueryBuildException("Failed to check if table [" + tableName + "] exists!", e);
            }
        }
        else {
            log.error("Unknown table_create_mode [" + tableCreateMode + "]!");
        }
    }

    @Override
    public IQuery createQuery(String query) throws PersistQueryBuildException {
        return null;
    }

    @Override
    public <T> IParameterizedQuery<T> createQuery(String query, Class<T> cls) throws PersistQueryBuildException {
        return null;
    }

    private Object executeQuery(String query, AnalyzedPropertyClass returnType, Class<?>[] parameterTypes, Object[] parameters){
        log.info("Executing [{}]", query);

//        DBEntityInfo entityInfo = entityInfos.get(returnType);
//        try {
//            DBHelper.updateEntity(databaseConnection, "PERSON", returnType, entityInfo, returnType.getBuilder().build());
//        }
//        catch (PersistException | SQLException | InvocationTargetException e){
//            e.printStackTrace();
//        }

//        DBHelper.test(databaseConnection);

        try {
            PreparedStatement statement = databaseConnection.prepareStatement(query);
            for(int i=0;i<parameterTypes.length;i++){
                DBHelper.prepareStatement(statement, i + 1, parameterTypes[i], parameters[i]);
            }

            //1. Is collection
            //2. Is array
            //3. Is Object-Properties - Map
            //4. Is Object
            boolean _set = false, _list = false, _array = false, _objPropMap = false, _obj = false;


            AnalyzedPropertyClass returnValueType;

            //1. COLLECTION (Set)
            if (Set.class.isAssignableFrom(returnType.getType())) {
                _set = true;
                AnalyzedParameterizedType parameterizedType = returnType.getParameterizedTypeMap().get("E");
                try {
                    returnValueType = analyzer.analyzeProperty(parameterizedType, parameterizedType.getType());
                }
                catch (ClassAnalyzeException e){
                    throw new MagicRuntimeException("Failed to analyze return type of set [" + parameterizedType + "]", e);
                }
            }
            //1. COLLECTION (List)
            else if (List.class.isAssignableFrom(returnType.getType())) {
                _list = true;
                AnalyzedParameterizedType parameterizedType = returnType.getParameterizedTypeMap().get("E");
                try {
                    returnValueType = analyzer.analyzeProperty(parameterizedType, parameterizedType.getType());
                }
                catch (ClassAnalyzeException e){
                    throw new MagicRuntimeException("Failed to analyze return type of list [" + parameterizedType + "]", e);
                }
            }

            //2. ARRAY
            else if (returnType.getType().isArray()) {
                _array = true;
                Class<?> componentType = returnType.getType().getComponentType();
                try {
                    returnValueType = analyzer.analyzeProperty(componentType, returnType.getParameterizedTypeMap());
                }
                catch (ClassAnalyzeException e){
                    throw new MagicRuntimeException("Failed to analyze return type of array [" + componentType + "]", e);
                }
            }

            //3. OBJECT-PROPERTY - MAP
            else if (Map.class.isAssignableFrom(returnType.getType())) {
                _objPropMap = true;
                //TODO
                returnValueType = returnType;
            }

            //4. OBJECT
            else {
                _obj = true;
                returnValueType = returnType;
            }


            if(returnValueType.getBuilder() == null) throw new MagicRuntimeException("No Builder present for return type [" + returnValueType.getType() + "]");






            ResultSet resultSet = statement.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int cols = metaData.getColumnCount();

            List<Map<String, Object>> dataMapList = new ArrayList<>();

            while(resultSet.next()){
                Map<String, Object> dataMap = new HashMap<>();
                for(int i=1;i<=cols;i++){
                    String colName = metaData.getColumnName(i);
                    int colType = metaData.getColumnType(i);

                    Object data = DBHelper.getDataFromDBType(resultSet, i, colType);
                    dataMap.put(colName, data);
                }

                dataMapList.add(dataMap);
            }

            if(_set){
                Set<Object> set = new HashSet<>();
                for(Map<String, Object> dataMap : dataMapList){
                    Object data;
                    try {
                        data = buildEntityFromDataMap(dataMap, returnValueType);
                    }
                    catch (PersistException e){
                        throw new MagicRuntimeException("Failed to build entity [" + returnValueType.getType() + "] from data map!", e);
                    }
                    set.add(data);
                }

                return set;
            }
            else if(_list){
                List<Object> list = new ArrayList<>();
                for(Map<String, Object> dataMap : dataMapList){
                    Object data;
                    try {
                        data = buildEntityFromDataMap(dataMap, returnValueType);
                    }
                    catch (PersistException e){
                        throw new MagicRuntimeException("Failed to build entity [" + returnValueType.getType() + "] from data map!", e);
                    }
                    list.add(data);
                }

                return list;
            }
            else if(_array){
                Object[] array = new Object[dataMapList.size()];
                int i=0;
                for(Map<String, Object> dataMap : dataMapList){
                    Object data;
                    try {
                        data = buildEntityFromDataMap(dataMap, returnValueType);
                    }
                    catch (PersistException e){
                        throw new MagicRuntimeException("Failed to build entity [" + returnValueType.getType() + "] from data map!", e);
                    }
                    array[i++] = data;
                }

                return array;
            }
            else if(_objPropMap){
                if(dataMapList.size() == 0) return null;
                if(dataMapList.size() > 1) return new PersistQueryExecuteException("Multiple entities returned but only 1 expected!");

                return dataMapList.get(0);
            }
            else {
                if(dataMapList.size() == 0) return null;
                if(dataMapList.size() > 1) return new PersistQueryExecuteException("Multiple entities returned but only 1 expected!");

                try {
                    return buildEntityFromDataMap(dataMapList.get(0), returnValueType);
                }
                catch (PersistException e){
                    throw new MagicRuntimeException("Failed to build entity [" + returnValueType.getType() + "] from data map!", e);
                }
            }

        }
        catch (SQLException e){
            throw new MagicRuntimeException("Failed to run sql statement: [" + query + "]!", e);
        }
    }

    private Object buildEntityFromDataMap(Map<String, Object> dataMap, AnalyzedPropertyClass cls) throws PersistException{
        Object dataObj;
        try {
            dataObj = cls.getBuilder().build();
        }
        catch (InvocationTargetException e){
            throw new PersistException("Failed to call builder [" + cls.getBuilder() + "]", e);
        }

        Map<String, PropertyEntry> fieldEntries = cls.getFieldEntryMap();
        for(String fieldName : fieldEntries.keySet()){
            String _dbFieldName = DBHelper.dbFieldName(fieldName);
            log.info("Field [{}] -> {}", fieldName, fieldEntries.get(fieldName).getType());

            PropertyEntry propertyEntry = fieldEntries.get(fieldName);
            Object data = dataMap.get(_dbFieldName);
            try {
                propertyEntry.getSetter().set(dataObj, data);
            }
            catch (InvocationTargetException e){
                throw new PersistException("Failed to set property of field [" + fieldName + "] of Entity [" + cls.getType() + "]", e);
            }
        }

        return dataObj;
    }

    private AnalyzedPropertyClass analyzeMethodReturnType(Method method, Map<String, AnalyzedParameterizedType> genericTypes) throws PersistQueryBuildException{
        Class<?> returnType = method.getReturnType();
        if(returnType == Void.class || returnType == Void.TYPE) return null;

        AnalyzedPropertyClass ret;
        try {
            ret = analyzer.analyzeProperty(returnType, method.getGenericReturnType(), genericTypes);
        }
        catch (ClassAnalyzeException e){
            throw new PersistQueryBuildException("Failed to analyze return type of method: [" + method + "]", e);
        }

        if(ret.getBuilder() == null && !ret.getType().isArray()) throw new PersistQueryBuildException("Failed to find builder for returning property class [" + method.getReturnType() + "] of method: [" + method + "]!");

        return ret;
    }

}
