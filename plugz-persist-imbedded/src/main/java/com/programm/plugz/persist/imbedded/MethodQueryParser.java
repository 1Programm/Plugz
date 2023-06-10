package com.programm.plugz.persist.imbedded;

import com.programm.plugz.api.utils.ValueUtils;
import com.programm.plugz.cls.analyzer.AnalyzedPropertyClass;
import com.programm.plugz.cls.analyzer.PropertyEntry;
import com.programm.plugz.persist.CustomQuery;
import com.programm.plugz.persist.Generated;
import com.programm.plugz.persist.PersistEntityInfo;
import com.programm.plugz.persist.PersistForeignKeyInfo;
import com.programm.plugz.persist.ex.PersistQueryBuildException;
import com.programm.plugz.persist.ex.PersistQueryExecuteException;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class MethodQueryParser {

    private static final String[] SELECT_METHOD_NAME_VARIANTS = {
            "findAll",
            "find",
            "getAll",
            "get"
    };

    private static final String[] UPDATE_METHOD_NAME_VARIANTS = {
            "update",
            "save"
    };

    private static final String[] DELETE_METHOD_NAME_VARIANTS = {
            "delete",
            "remove"
    };

    private static class StaticMethodQueryInfoSupplier implements MethodQueryInfoSupplier {
        StatementType type;
        String query;
        AnalyzedPropertyClass returnType;
        final List<Class<?>> parameterTypes = new ArrayList<>();

        @Override
        public MethodQueryInfo queryInfo(Object[] parameters) {
            return new MethodQueryInfo(type, query, returnType, parameterTypes, parameters, null);
        }
    }

    @RequiredArgsConstructor
    private static class UpdateCreateMethodInfoSupplier implements MethodQueryInfoSupplier {
        final PersistEntityInfo entityInfo;
        final Map<Class<?>, PersistEntityInfo> infoMap;
        final AnalyzedPropertyClass returnType;

        String create_query;
        final List<String> create_fieldOrder = new ArrayList<>();
        final List<Class<?>> create_parameterTypes = new ArrayList<>();

        String update_query;
        final List<String> update_fieldOrder = new ArrayList<>();
        final List<Class<?>> update_parameterTypes = new ArrayList<>();

        @Override
        public MethodQueryInfo queryInfo(Object[] parameters) {
            if(parameters.length == 0 || parameters[0].getClass() != entityInfo.entityClass) throw new PersistQueryExecuteException("INVALID STATE: invalid input");

            Object entityObject = parameters[0];
            PropertyEntry idFieldEntry = entityInfo.analyzedEntity.getFieldEntryMap().get(entityInfo.primaryKey);

            Object idValue;
            try {
                idValue = idFieldEntry.getGetter().get(entityObject);
            }
            catch (InvocationTargetException e){
                throw new PersistQueryExecuteException("Could not get the id field for entity [" + entityInfo.entityClass + "]", e);
            }

            Map<String, PropertyEntry> fieldEntries = entityInfo.analyzedEntity.getFieldEntryMap();
            if(idValue == null || idValue.equals(ValueUtils.getDefaultValue(idFieldEntry.getType()))){
                Object[] mappedParameters = new Object[create_fieldOrder.size()];
                for(int i=0;i<create_fieldOrder.size();i++){
                    String fieldName = create_fieldOrder.get(i);
                    PropertyEntry fieldEntry = fieldEntries.get(fieldName);

                    PersistForeignKeyInfo foreignKeyInfo = entityInfo.foreignKeyInfoMap.get(fieldName);
                    if(foreignKeyInfo != null){
                        PersistEntityInfo foreignEntityInfo = infoMap.get(foreignKeyInfo.foreignEntityType);
                        PropertyEntry foreignKeyMappedFieldEntry = foreignEntityInfo.analyzedEntity.getFieldEntryMap().get(foreignKeyInfo.foreignKey);
                        try {
                            Object fieldValue = fieldEntry.getGetter().get(entityObject);
                            if(fieldValue == null) continue;

                            Object foreignKeyMappedFieldValue = foreignKeyMappedFieldEntry.getGetter().get(fieldValue);
                            mappedParameters[i] = foreignKeyMappedFieldValue;
                        }
                        catch (InvocationTargetException e){
                            throw new PersistQueryExecuteException("Could not get field [" + fieldName + "] of entity [" + entityInfo.entityClass + "]!", e);
                        }
                    }
                    else {
                        try {
                            Object fieldValue = fieldEntry.getGetter().get(entityObject);
                            mappedParameters[i] = fieldValue;
                        }
                        catch (InvocationTargetException e){
                            throw new PersistQueryExecuteException("Could not get field [" + fieldName + "] of entity [" + entityInfo.entityClass + "]!", e);
                        }
                    }
                }

                GeneratedKeysCallback generatedKeysCallback = null;

                if(idFieldEntry.getSetter() != null){
                    generatedKeysCallback = (keys) -> {
                        ResultSetMetaData metaData = keys.getMetaData();
                        if(keys.next()){
                            int type = metaData.getColumnType(1);
                            Object generatedId = DBHelper.getDataFromDBType(keys, 1, type);

                            try {
                                idFieldEntry.getSetter().set(entityObject, generatedId);
                            } catch (InvocationTargetException e) {
                                throw new PersistQueryExecuteException("Failed to set the generated id value for entity [" + entityInfo.entityClass + "]!", e);
                            }
                        }
                        else {
                            throw new SQLException("Failed to get generated ID.");
                        }
                    };
                }
                else {
                    System.err.println("WARNING: No setter available for id field of entity [" + entityInfo.entityClass + "]!");
                    //TODO: Log warning
                }

                //TODO
                return new MethodQueryInfo(StatementType.UPDATE, create_query, returnType, create_parameterTypes, mappedParameters, generatedKeysCallback);
            }
            else {
                Object[] mappedParameters = new Object[update_fieldOrder.size()];
                for(int i=0;i<update_fieldOrder.size();i++){
                    String fieldName = update_fieldOrder.get(i);
                    PropertyEntry fieldEntry = fieldEntries.get(fieldName);
                    try {
                        Object fieldValue = fieldEntry.getGetter().get(entityObject);
                        mappedParameters[i] = fieldValue;
                    }
                    catch (InvocationTargetException e){
                        throw new PersistQueryExecuteException("Could not get field [" + fieldName + "] of entity [" + entityInfo.entityClass + "]!", e);
                    }
                }

                return new MethodQueryInfo(StatementType.UPDATE, update_query, returnType, update_parameterTypes, mappedParameters, null);
            }
        }
    }

    //delete(Person) -> DELETE FROM PERSON WHERE id = ?
    @RequiredArgsConstructor
    private static class DeleteMethodQueryInfoSupplier implements MethodQueryInfoSupplier {
        final PersistEntityInfo entityInfo;

        String query;
        AnalyzedPropertyClass returnType;
        final List<Class<?>> parameterTypes = new ArrayList<>();

        @Override
        public MethodQueryInfo queryInfo(Object[] parameters) {
            if(parameters.length == 0 || parameters[0].getClass() != entityInfo.entityClass) throw new PersistQueryExecuteException("INVALID STATE: invalid input");

            Object entityObject = parameters[0];
            PropertyEntry idFieldEntry = entityInfo.analyzedEntity.getFieldEntryMap().get(entityInfo.primaryKey);

            Object idValue;
            try {
                idValue = idFieldEntry.getGetter().get(entityObject);
            }
            catch (InvocationTargetException e){
                throw new PersistQueryExecuteException("Could not get the id field for entity [" + entityInfo.entityClass + "]", e);
            }

            Object[] idParameterArray = new Object[1];
            idParameterArray[0] = idValue;

            return new MethodQueryInfo(StatementType.UPDATE, query, returnType, parameterTypes, idParameterArray, null);
        }
    }






    public static MethodQueryInfoSupplier parse(String tableName, PersistEntityInfo entityInfo, Method method, AnalyzedPropertyClass returnType, Map<Class<?>, PersistEntityInfo> infoMap) throws PersistQueryBuildException{
        CustomQuery customQueryAnnotation = method.getAnnotation(CustomQuery.class);
        String customQuery = customQueryAnnotation == null ? null : customQueryAnnotation.value();
        return analyzeAndGenerateSqlFromMethodName(customQuery, tableName, entityInfo, method.getName(), returnType, infoMap);
    }

    public static MethodQueryInfoSupplier analyzeAndGenerateSqlFromMethodName(String customQuery, String tableName, PersistEntityInfo entityInfo, String mName, AnalyzedPropertyClass returnType, Map<Class<?>, PersistEntityInfo> infoMap) throws PersistQueryBuildException{
        //GET / FIND
        for(String selectStart : SELECT_METHOD_NAME_VARIANTS){
            if(mName.startsWith(selectStart)){
                StaticMethodQueryInfoSupplier staticSupplier = new StaticMethodQueryInfoSupplier();

                staticSupplier.type = StatementType.QUERY;
                staticSupplier.returnType = returnType;

                StringBuilder sb = new StringBuilder();
                sb.append("SELECT ");
                addSelection(staticSupplier, entityInfo, sb, tableName, mName, selectStart.length());
                staticSupplier.query = customQuery == null ? sb.toString() : customQuery;

                return staticSupplier;
            }
        }


        for(String updateStart : UPDATE_METHOD_NAME_VARIANTS){
            if(mName.startsWith(updateStart)){
                UpdateCreateMethodInfoSupplier updateCreateSupplier = new UpdateCreateMethodInfoSupplier(entityInfo, infoMap, returnType);

                StringBuilder sbCreate = new StringBuilder();
                sbCreate.append("INSERT INTO ").append(tableName).append("(");

                addCreate(updateCreateSupplier, entityInfo, sbCreate, mName, updateStart.length(), infoMap);
                updateCreateSupplier.create_query = sbCreate.toString();

                StringBuilder sbUpdate = new StringBuilder();
                sbUpdate.append("UPDATE ").append(tableName).append(" SET ");

                addUpdate(updateCreateSupplier, entityInfo, sbUpdate, mName, updateStart.length(), infoMap);
                updateCreateSupplier.update_query = sbUpdate.toString();

                return updateCreateSupplier;
            }
        }

        for(String deleteStart : DELETE_METHOD_NAME_VARIANTS){
            if(mName.startsWith(deleteStart)){
                DeleteMethodQueryInfoSupplier infoSupplier = new DeleteMethodQueryInfoSupplier(entityInfo);
                infoSupplier.returnType = returnType;

                StringBuilder sb = new StringBuilder();
                sb.append("DELETE FROM ").append(tableName).append(" WHERE ");

                addDelete(infoSupplier, entityInfo, sb, mName, deleteStart.length());

                infoSupplier.query = sb.toString();

                return infoSupplier;
            }
        }

        throw new PersistQueryBuildException("INVALID STATE");
    }

//    public static void main(String[] args) {
//        String[] tests = {
//                "getAll",
//                "findAll",
//                "getAllByName",
//                "findAllNameByAge",
//                "findNameAndAgeByIdOrNameAndAge"
//        };
//
//
//        for(String test : tests){
//            String sql = generateSqlStatementFromMethodName("person", test);
//            System.out.println("[" + test + "] " + " ".repeat(Math.max(0, 20 - test.length())) + "-> " + sql);
//        }
//    }

    private static void addSelection(StaticMethodQueryInfoSupplier supplier, PersistEntityInfo entityInfo, StringBuilder sb, String tableName, String s, int index) throws PersistQueryBuildException{
        int indexOfBy = indexOfLowercase(s, "by", index);

        int endSelection = indexOfBy == -1 ? s.length() : indexOfBy;
        String _selection = s.substring(index, endSelection);
        if(!_selection.isEmpty()) {
            String[] selection = _selection.split("And");
            for(int i=0;i<selection.length;i++){
                if(i != 0) sb.append(", ");
                sb.append(selection[i].toLowerCase());
            }
        }
        else {
            sb.append("*");
        }

        sb.append(" FROM ").append(tableName);

        if(indexOfBy != -1){
            sb.append(" WHERE ");
            String _whereFields = s.substring(indexOfBy + 2);
            String[] whereFields = _whereFields.split("And");
            for(int i=0;i<whereFields.length;i++){
                if(i != 0) sb.append(" AND ");
                String[] whereOrSplit = whereFields[i].split("Or");
                if(whereOrSplit.length > 1) sb.append("(");
                for(int o=0;o<whereOrSplit.length;o++){
                    if(o != 0) sb.append(" OR ");
                    String colName = whereOrSplit[o].toLowerCase();
                    sb.append(colName).append(" = ?");

                    Class<?> type = getParameterTypeByColName(entityInfo.analyzedEntity, colName);
                    if(type == null) throw new PersistQueryBuildException("Failed to get field of entity [" + entityInfo.entityClass + "] for column name [" + colName + "]!");
                    supplier.parameterTypes.add(type);
                }
                if(whereOrSplit.length > 1) sb.append(")");
            }
        }
    }

    private static void addCreate(UpdateCreateMethodInfoSupplier infoSupplier, PersistEntityInfo entityInfo, StringBuilder sb, String s, int index, Map<Class<?>, PersistEntityInfo> infoMap) throws PersistQueryBuildException{
        if(s.length() == index){
            Map<String, PropertyEntry> fieldEntries = entityInfo.analyzedEntity.getFieldEntryMap();
//            String[] fieldNameOrder = new String[fieldEntries.size()];

            int i = 0;
            for(String fieldName : fieldEntries.keySet()){
                PropertyEntry entry = fieldEntries.get(fieldName);
                Generated isGeneratedByDb = entry.getField().getAnnotation(Generated.class);
                if(fieldName.equals(entityInfo.primaryKey) || isGeneratedByDb != null) continue;

                String dbFieldName = DBHelper.dbFieldName(fieldName);

                PersistForeignKeyInfo foreignKeyInfo = entityInfo.foreignKeyInfoMap.get(fieldName);
                if(foreignKeyInfo != null){
                    PersistEntityInfo foreignEntityInfo = infoMap.get(foreignKeyInfo.foreignEntityType);
                    dbFieldName = DBHelper.getForeignKeyFromInfo(dbFieldName, foreignKeyInfo);
                    entry = foreignEntityInfo.analyzedEntity.getFieldEntryMap().get(foreignKeyInfo.foreignKey);
                }

//                if(fieldName.equals(entityInfo.primaryKey) && entityInfo.primaryKeyGenerated) continue;

                if(i != 0) sb.append(",");
                sb.append(dbFieldName);

                infoSupplier.create_parameterTypes.add(entry.getType());
                infoSupplier.create_fieldOrder.add(fieldName);

                i++;
            }

            sb.append(") VALUES(");

            for(int o=0;o<i;o++){
                if(o != 0) sb.append(",");
                sb.append("?");
            }

            sb.append(")");
            return;
        }

        throw new PersistQueryBuildException("Input [" + s + "] does not match the expected patterns for building sql queries!");
    }

    private static void addUpdate(UpdateCreateMethodInfoSupplier infoSupplier, PersistEntityInfo entityInfo, StringBuilder sb, String s, int index, Map<Class<?>, PersistEntityInfo> infoMap) throws PersistQueryBuildException{
        if(s.length() == index){
            Map<String, PropertyEntry> fieldEntries = entityInfo.analyzedEntity.getFieldEntryMap();

            int i = 0;
            for(String fieldName : fieldEntries.keySet()){
                if(fieldName.equals(entityInfo.primaryKey)) continue;

                PropertyEntry entry = fieldEntries.get(fieldName);
                String dbFieldName = DBHelper.dbFieldName(fieldName);

                PersistForeignKeyInfo foreignKeyInfo = entityInfo.foreignKeyInfoMap.get(fieldName);
                if(foreignKeyInfo != null){
                    PersistEntityInfo foreignEntityInfo = infoMap.get(foreignKeyInfo.foreignEntityType);
                    dbFieldName = DBHelper.getForeignKeyFromInfo(dbFieldName, foreignKeyInfo);
                    entry = foreignEntityInfo.analyzedEntity.getFieldEntryMap().get(foreignKeyInfo.foreignKey);
                }

                if(i != 0) sb.append(", ");

                sb.append(dbFieldName).append(" = ?");

                infoSupplier.update_parameterTypes.add(entry.getType());
                infoSupplier.update_fieldOrder.add(fieldName);

                i++;
            }

            sb.append(" WHERE ").append(entityInfo.primaryKey).append(" = ?");

            PropertyEntry primaryKeyFieldEntry = fieldEntries.get(entityInfo.primaryKey);
            infoSupplier.update_parameterTypes.add(primaryKeyFieldEntry.getType());
            infoSupplier.update_fieldOrder.add(entityInfo.primaryKey);
            return;
        }

        throw new PersistQueryBuildException("Input [" + s + "] does not match the expected patterns for building sql queries!");
    }

    private static void addDelete(DeleteMethodQueryInfoSupplier infoSupplier, PersistEntityInfo entityInfo, StringBuilder sb, String s, int index) throws PersistQueryBuildException{
        if(s.length() == index) {
            Map<String, PropertyEntry> fieldEntries = entityInfo.analyzedEntity.getFieldEntryMap();
            PropertyEntry primaryKeyFieldEntry = fieldEntries.get(entityInfo.primaryKey);

            infoSupplier.parameterTypes.add(primaryKeyFieldEntry.getType());
            sb.append(entityInfo.primaryKey).append(" = ?");
            return;
        }

        throw new PersistQueryBuildException("Input [" + s + "] does not match the expected patterns for building sql queries!");
    }

    private static Class<?> getParameterTypeByColName(AnalyzedPropertyClass entityClass, String colName){
        Class<?> type = null;

        Map<String, PropertyEntry> fields = entityClass.getFieldEntryMap();
        for(String fieldName : fields.keySet()){
            if(fieldName.equalsIgnoreCase(colName)){
                PropertyEntry entry = fields.get(fieldName);
                type = entry.getType();
                break;
            }
        }

        return type;
    }

    private static int indexOfLowercase(String s, String find, int index){
        int len = s.length() - (find.length()-1);
        outerLoop:
        for(;index<len;index++){
            for(int i=0;i<find.length();i++){
                char c1 = Character.toLowerCase(s.charAt(index + i));
                char c2 = Character.toLowerCase(find.charAt(i));
                if(c1 != c2) continue outerLoop;
            }

            return index;
        }

        return -1;
    }

}
