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

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;

@Logger("Persist [Embedded]")
class EmbeddedRepoHandler implements IRepoHandler {

    private static String getUniqueMethodName(Method method){
        return method.getName() + "%" + method.toGenericString();
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
        private final Class<?> repoClass;
        private final Map<String, MethodQueryInfoSupplier> methodQueryMap;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String mName = method.getName();
            String mNameUnique = getUniqueMethodName(method);

            switch (mName){
                case "getClass":
                    return repoClass;
                case "equals":
                    return false;
                case "toString":
                    return repoClass.getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "notify":
                case "notifyAll":
                case "wait":
                    throw new OperationNotSupportedException("Operation [" + mName + "] is not supported for repo proxy!");
            }

            MethodQueryInfoSupplier infoSupplier = methodQueryMap.get(mNameUnique);
            if(infoSupplier == null) throw new PersistQueryExecuteException("INVALID STATE: No query defined for method [" + mNameUnique + "]!");

            return executeQuery(infoSupplier, args);
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

    private static final String CONF_PERSITS_DB_LOG_STATEMENTS_NAME = "persist.db.log.statements";
    private static final boolean CONF_PERSITS_DB_LOG_STATEMENTS_DEFAULT = false;





    private final ILogger log;
    private final IInstanceManager instanceManager;

    private final String databaseUrl;
    private final String databaseUsername;
    private final String databasePassword;
    private final String tableCreateMode;

    private final boolean logStatements;

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

        this.logStatements = config.getBoolOrDefault(CONF_PERSITS_DB_LOG_STATEMENTS_NAME, CONF_PERSITS_DB_LOG_STATEMENTS_DEFAULT);
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
        analyzer.registerBuilderProvider(this::provideBuilders);

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


        Map<String, MethodQueryInfoSupplier> methodQueryMap = new HashMap<>();
        Method[] declaredMethods = cls.getDeclaredMethods();
        for(Method method : declaredMethods){
            AnalyzedPropertyClass methodReturnType = analyzeMethodReturnType(method, analyzedEntityClass.getParameterizedTypeMap());

            String mName = getUniqueMethodName(method);

            try {
                MethodQueryInfoSupplier infoSupplier = MethodQueryParser.parse(tableName, analyzedEntityClass, entityInfo, method, methodReturnType);
                methodQueryMap.put(mName, infoSupplier);
            }
            catch (PersistQueryBuildException e){
                throw new PersistQueryBuildException("Failed to analyze and parse method [" + method + "] into a sql statement!", e);
            }
        }

        EmbeddedInvocationHandler invocationHandler = new EmbeddedInvocationHandler(cls, methodQueryMap);
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
            throw new PersistQueryBuildException("Unknown table_create_mode [" + tableCreateMode + "]!");
        }
    }

    @Override
    public IQuery createQuery(String query) throws PersistQueryBuildException {
        //TODO: Check if needed - otherwise remove
        return null;
    }

    @Override
    public <T> IParameterizedQuery<T> createQuery(String query, Class<T> cls) throws PersistQueryBuildException {
        //TODO: Check if needed - otherwise remove
        return null;
    }

    private Object executeQuery(MethodQueryInfoSupplier infoSupplier, Object[] origParameters){
        MethodQueryInfo info = infoSupplier.queryInfo(origParameters);
        Object[] parameters = info.statementArguments;

        if(logStatements) log.info("Executing [{}] with parameters: {}", info.query, Arrays.toString(parameters));



        //Prepare Statement
        PreparedStatement statement;
        try {
            statement = databaseConnection.prepareStatement(info.query);
            for (int i = 0; i < info.parameterTypes.size(); i++) {
                DBHelper.prepareStatement(statement, i + 1, info.parameterTypes.get(i), parameters[i]);
            }
        }
        catch (SQLException e){
            throw new PersistQueryExecuteException("Failed to prepare statement for query [" + info.query + "]!", e);
        }





        //Check what return type
        AnalyzedPropertyClass retType = info.returnType;
        AnalyzedPropertyClass returnValueType = null;
        QueryResultType resultType = null;

        if(retType != null){
            //1. COLLECTION (Set)
            if (Set.class.isAssignableFrom(retType.getType())) {
                resultType = QueryResultType.SET;
                AnalyzedParameterizedType parameterizedType = retType.getParameterizedTypeMap().get("E");
                try {
                    returnValueType = analyzer.analyzeProperty(parameterizedType, parameterizedType.getType());
                }
                catch (ClassAnalyzeException e){
                    throw new MagicRuntimeException("Failed to analyze return type of set [" + parameterizedType + "]", e);
                }
            }
            //1. COLLECTION (List)
            else if (List.class.isAssignableFrom(retType.getType())) {
                resultType = QueryResultType.LIST;
                AnalyzedParameterizedType parameterizedType = retType.getParameterizedTypeMap().get("E");
                try {
                    returnValueType = analyzer.analyzeProperty(parameterizedType, parameterizedType.getType());
                }
                catch (ClassAnalyzeException e){
                    throw new MagicRuntimeException("Failed to analyze return type of list [" + parameterizedType + "]", e);
                }
            }

            //2. ARRAY
            else if (retType.getType().isArray()) {
                resultType = QueryResultType.ARRAY;
                Class<?> componentType = retType.getType().getComponentType();
                try {
                    returnValueType = analyzer.analyzeProperty(componentType, retType.getParameterizedTypeMap());
                }
                catch (ClassAnalyzeException e){
                    throw new MagicRuntimeException("Failed to analyze return type of array [" + componentType + "]", e);
                }
            }

            //3. OBJECT-PROPERTY - MAP
            else if (Map.class.isAssignableFrom(retType.getType())) {
                resultType = QueryResultType.OBJECT_MAP;
                returnValueType = retType;
            }

            //4. OBJECT
            else {
                resultType = QueryResultType.OBJECT;
                returnValueType = retType;
            }

            if(returnValueType.getBuilder() == null) throw new MagicRuntimeException("No Builder present for return type [" + returnValueType.getType() + "]");
        }








        //Check if executeQuery or executeUpdate should be called
        if(info.type == StatementType.QUERY){
            try {
                return executeQueryFromPreparedStatement(statement, returnValueType, resultType);
            }
            catch (SQLException e){
                throw new MagicRuntimeException("Failed to run sql statement: [" + info.query + "]!", e);
            }
        }
        else if(info.type == StatementType.UPDATE){
            try {
                executeUpdateFromPreparedStatement(statement);
                return null;
            }
            catch (SQLException e){
                throw new MagicRuntimeException("Failed to run sql statement: [" + info.query + "]!", e);
            }
        }


        throw new IllegalStateException("INVALID STATE: Unchecked type: [" + info.type + "]!");
    }

    private Object executeQueryFromPreparedStatement(PreparedStatement statement, AnalyzedPropertyClass returnValueType, QueryResultType resultType) throws SQLException{
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

        if(resultType == QueryResultType.SET){
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
        else if(resultType == QueryResultType.LIST){
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
        else if(resultType == QueryResultType.ARRAY){
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
        else if(resultType == QueryResultType.OBJECT_MAP){
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

    private void executeUpdateFromPreparedStatement(PreparedStatement statement) throws SQLException {
        statement.executeUpdate();
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
