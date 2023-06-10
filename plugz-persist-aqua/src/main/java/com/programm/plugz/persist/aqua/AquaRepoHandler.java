package com.programm.plugz.persist.aqua;

import com.programm.ioutils.log.api.ILogger;
import com.programm.ioutils.log.api.Logger;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.cls.analyzer.AnalyzedParameterizedType;
import com.programm.plugz.cls.analyzer.AnalyzedPropertyClass;
import com.programm.plugz.cls.analyzer.ClassAnalyzeException;
import com.programm.plugz.cls.analyzer.ClassAnalyzer;
import com.programm.plugz.persist.CustomQuery;
import com.programm.plugz.persist.IRepoHandler;
import com.programm.plugz.persist.PersistEntityInfo;
import com.programm.plugz.persist.ex.PersistQueryBuildException;
import com.programm.plugz.persist.ex.PersistQueryExecuteException;
import com.programm.plugz.persist.ex.PersistShutdownException;
import com.programm.plugz.persist.ex.PersistStartupException;
import com.programm.plugz.persist.query.IParameterizedQuery;
import com.programm.plugz.persist.query.IQuery;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;


/*
Aqua sql statements:

find(*, (name = Peter & age = 10) | id = 1)
find({name, age}, (name = Peter & age = 10) | id = 1)
findAll()

find(*, diff(name, Peter, 2))


 */
@Logger("Persist [Aqua]")
public class AquaRepoHandler implements IRepoHandler {

    @RequiredArgsConstructor
    private class AquaQueryImpl implements IQuery {
        private final QueryExecutionInfo executionInfo;

        @Override
        public Object execute(Object... args) throws PersistQueryExecuteException {
            return executeQuery(executionInfo, null, null, null, null, args);
        }
    }

    private final ILogger log;
    private final ClassAnalyzer analyzer = new ClassAnalyzer(true, true, true);
    private final AquaQueryBuilder queryBuilder = new AquaQueryBuilder();
    //TODO
    private IAquaRepositoryConnection repoConnection = null;

    public AquaRepoHandler(@Get ILogger log) {
        this.log = log;
    }

    @Override
    public void startup(ClassAnalyzer analyzer, Map<Class<?>, PersistEntityInfo> infoMap) throws PersistStartupException {
        log.info("Starting up Aqua repo handler...");
        //TODO ...
    }

    @Override
    public void shutdown() throws PersistShutdownException {
        log.debug("Shutting down current database connection...");
        //TODO ...
    }

    @Override
    public Object createRepoImplementation(Class<?> repoCls, PersistEntityInfo entityInfo, Map<Class<?>, PersistEntityInfo> infoMap) throws PersistQueryBuildException {
        Map<String, AquaQueryInfo> methodToQueryMap = null;//analyzeClass(cls, analyzedEntityClass);
        AquaRepoInvocationHandler invocationHandler = new AquaRepoInvocationHandler(methodToQueryMap, this);
        return Proxy.newProxyInstance(repoCls.getClassLoader(), new Class<?>[]{repoCls}, invocationHandler);
    }

    private Map<String, AquaQueryInfo> analyzeClass(Class<?> cls, AnalyzedPropertyClass analyzedEntityClass) throws PersistQueryBuildException {
        Map<String, AquaQueryInfo> methodToQueryMap = new HashMap<>();

        Method[] declaredMethods = cls.getDeclaredMethods();
        for(Method method : declaredMethods){
            AquaQueryInfo query = analyzeMethodForQuery(method, analyzedEntityClass);

            String methodName = method.toString();
            methodToQueryMap.put(methodName, query);
        }

        return methodToQueryMap;
    }

    private AquaQueryInfo analyzeMethodForQuery(Method method, AnalyzedPropertyClass analyzedEntityClass) throws PersistQueryBuildException{
        String name = method.getName();
        CustomQuery customQueryAnnotation = method.getAnnotation(CustomQuery.class);
        if(customQueryAnnotation != null) {
            return analyzeCustomQuery(customQueryAnnotation.value(), method, analyzedEntityClass);
        }

        if(name.startsWith("getAll")){
            return createFindAllQuery(name.substring("getAll".length()), method, analyzedEntityClass);
        }
        else if(name.startsWith("findAll")){
            return createFindAllQuery(name.substring("findAll".length()), method, analyzedEntityClass);
        }
        else if(name.startsWith("get")){
            return createFindQuery(name.substring("get".length()), method, analyzedEntityClass);
        }
        else if(name.startsWith("find")){
            return createFindQuery(name.substring("find".length()), method, analyzedEntityClass);
        }
        else {
            throw new PersistQueryBuildException("Invalid name for method [" + method + "] to build a query.");
        }
    }

    private AquaQueryInfo analyzeCustomQuery(String _query, Method method, AnalyzedPropertyClass analyzedEntityClass) throws PersistQueryBuildException {
        Class<?> _returnType = method.getReturnType();
        Type _genericReturnType = method.getGenericReturnType();
        AnalyzedParameterizedType returnType;

        try {
            if(_genericReturnType != _returnType) {
                returnType = analyzer.analyzeParameterizedType(_returnType, _genericReturnType, Collections.emptyMap());
            }
            else {
                returnType = analyzer.analyzeParameterizedCls(_returnType);
            }
        }
        catch (ClassAnalyzeException e){
            throw new PersistQueryBuildException("Failed to analyze return type for query method [" + method + "].", e);
        }

        boolean array = false;
        boolean collection = false;

        if(_returnType.isArray()) {
            array = true;
        }
        else if(Set.class.isAssignableFrom(_returnType)){
            collection = true;
        }
        else if(List.class.isAssignableFrom(_returnType)){
            collection = true;
        }

        Class<?>[] parameters = method.getParameterTypes();
        Type[] genericParameters = method.getGenericParameterTypes();
        List<AnalyzedParameterizedType> parameterTypes = new ArrayList<>();

        for(int i=0;i<parameters.length;i++){
            Class<?> parameter = parameters[i];
            Type genericParameter = genericParameters[i];
            try {
                AnalyzedParameterizedType analyzedParameterizedType = analyzer.analyzeParameterizedType(parameter, genericParameter, Collections.emptyMap());
                parameterTypes.add(analyzedParameterizedType);
            }
            catch (ClassAnalyzeException e){
                throw new PersistQueryBuildException("Failed to analyze type of parameter[" + i + "] for query method [" + method + "].", e);
            }
        }


        String fullQuery = getTableName(analyzedEntityClass) + "." + _query;
        QueryExecutionInfo executionInfo = queryBuilder.parseQuery(fullQuery);
        return new AquaQueryInfo(executionInfo, returnType, array, collection, parameterTypes);
    }

    private AquaQueryInfo createFindAllQuery(String query, Method method, AnalyzedPropertyClass analyzedEntityClass) throws PersistQueryBuildException{
        String selection;

        if(query.isEmpty()){
            selection = "*";
        }
        else {
            String[] names = query.split("And");
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            for(int i=0;i<names.length;i++) {
                if(i != 0) sb.append(", ");
                sb.append(names[i].toLowerCase());
            }
            sb.append("}");

            selection = sb.toString();
        }

        AquaQueryInfo info = analyzeCustomQuery("find(" + selection + ")", method, analyzedEntityClass);

        if(!(info.array || info.collection)) throw new PersistQueryBuildException("[Find-All] queries must have either an array or a collection as return type!");
//        if(info.componentType != analyzedEntityClass.getType()) throw new PersistQueryBuildException("[Find-All] query expected [" + analyzedEntityClass.getType() + "] in collection - got [" + info.componentType + "].");
        if(info.parameterTypes.size() != 0) throw new PersistQueryBuildException("No input parameters expected for [Find-All] query.");

        return info;
    }

    private AquaQueryInfo createFindQuery(String s, Method method, AnalyzedPropertyClass analyzedEntityClass) throws PersistQueryBuildException{
        StringBuilder query = new StringBuilder("find(");
        int indexOfBy = s.indexOf("By");

        if(indexOfBy == 0){
            query.append("*");
        }
        else {
            String before = s.substring(0, indexOfBy);
            String[] andSplit = before.split("And");
            for(int i=0;i<andSplit.length;i++){
                if(i != 0) query.append(", ");
                query.append(andSplit[i].toLowerCase());
            }
        }

        query.append(", ");

        String rest = s.substring(indexOfBy + "By".length());
        String[] andSplit = rest.split("And");
        for(int i=0;i<andSplit.length;i++){
            String paramName = andSplit[i].toLowerCase();
            boolean found = false;
            for(String entityFieldName : analyzedEntityClass.getFieldEntryMap().keySet()){
                if(entityFieldName.toLowerCase().equals(paramName)){
                    found = true;
                    break;
                }
            }

            if(!found) throw new PersistQueryBuildException("No such field [" + paramName + "] found in entity class [" + analyzedEntityClass.getType() + "].");

            if(i != 0) query.append(" & ");
            query.append(paramName).append(" = $").append(i);
        }

        query.append(")");

        return analyzeCustomQuery(query.toString(), method, analyzedEntityClass);
    }

    private String getTableName(AnalyzedPropertyClass entityCls){
        return entityCls.getType().getSimpleName().toLowerCase();
    }

    public Object executeQuery(QueryExecutionInfo executionInfo, AnalyzedParameterizedType returnType, Boolean array, Boolean collection, List<AnalyzedParameterizedType> parameterTypes, Object[] args){
        Object result = repoConnection.execute(executionInfo.tableName, executionInfo.method, executionInfo.selections, executionInfo.conditions);

        return result;
    }
}
