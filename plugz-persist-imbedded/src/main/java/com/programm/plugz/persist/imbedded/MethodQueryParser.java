package com.programm.plugz.persist.imbedded;

import com.programm.plugz.api.MagicRuntimeException;
import com.programm.plugz.api.utils.ValueUtils;
import com.programm.plugz.cls.analyzer.AnalyzedPropertyClass;
import com.programm.plugz.cls.analyzer.PropertyEntry;
import com.programm.plugz.persist.CustomQuery;
import com.programm.plugz.persist.ex.PersistQueryBuildException;
import com.programm.plugz.persist.ex.PersistQueryExecuteException;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
            return new MethodQueryInfo(type, query, returnType, parameterTypes, parameters);
        }
    }

    @RequiredArgsConstructor
    private static class UpdateCreateMethodInfoSupplier implements MethodQueryInfoSupplier {
        final AnalyzedPropertyClass entityType;
        final DBEntityInfo entityInfo;

        final AnalyzedPropertyClass returnType;

        String create_query;
        final List<String> create_fieldOrder = new ArrayList<>();
        final List<Class<?>> create_parameterTypes = new ArrayList<>();

        String update_query;
        final List<String> update_fieldOrder = new ArrayList<>();
        final List<Class<?>> update_parameterTypes = new ArrayList<>();

        @Override
        public MethodQueryInfo queryInfo(Object[] parameters) {
            if(parameters.length == 0 || parameters[0].getClass() != entityType.getType()) throw new PersistQueryExecuteException("INVALID STATE: invalid input");

            Object entityObject = parameters[0];
            PropertyEntry idFieldEntry = entityType.getFieldEntryMap().get(entityInfo.primaryKey);

            Object idValue;
            try {
                idValue = idFieldEntry.getGetter().get(entityObject);
            }
            catch (InvocationTargetException e){
                throw new PersistQueryExecuteException("Could not get the id field for entity [" + entityType.getType() + "]", e);
            }

            Map<String, PropertyEntry> fieldEntries = entityType.getFieldEntryMap();
            if(idValue == ValueUtils.getDefaultValue(idFieldEntry.getType())){
                Object[] mappedParameters = new Object[create_fieldOrder.size()];
                for(int i=0;i<create_fieldOrder.size();i++){
                    String fieldName = create_fieldOrder.get(i);
                    PropertyEntry fieldEntry = fieldEntries.get(fieldName);
                    try {
                        Object fieldValue = fieldEntry.getGetter().get(entityObject);
                        mappedParameters[i] = fieldValue;
                    }
                    catch (InvocationTargetException e){
                        throw new PersistQueryExecuteException("Could not get field [" + fieldName + "] of entity [" + entityType + "]!", e);
                    }
                }

                return new MethodQueryInfo(StatementType.UPDATE, create_query, returnType, create_parameterTypes, mappedParameters);
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
                        throw new PersistQueryExecuteException("Could not get field [" + fieldName + "] of entity [" + entityType + "]!", e);
                    }
                }

                return new MethodQueryInfo(StatementType.UPDATE, update_query, returnType, update_parameterTypes, mappedParameters);
            }
        }
    }

    //delete(Person) -> DELETE FROM PERSON WHERE id = ?
    @RequiredArgsConstructor
    private static class DeleteMethodQueryInfoSupplier implements MethodQueryInfoSupplier {
        final AnalyzedPropertyClass entityType;
        final DBEntityInfo entityInfo;

        String query;
        AnalyzedPropertyClass returnType;
        final List<Class<?>> parameterTypes = new ArrayList<>();

        @Override
        public MethodQueryInfo queryInfo(Object[] parameters) {
            if(parameters.length == 0 || parameters[0].getClass() != entityType.getType()) throw new PersistQueryExecuteException("INVALID STATE: invalid input");

            Object entityObject = parameters[0];
            PropertyEntry idFieldEntry = entityType.getFieldEntryMap().get(entityInfo.primaryKey);

            Object idValue;
            try {
                idValue = idFieldEntry.getGetter().get(entityObject);
            }
            catch (InvocationTargetException e){
                throw new PersistQueryExecuteException("Could not get the id field for entity [" + entityType.getType() + "]", e);
            }

            Object[] idParameterArray = new Object[1];
            idParameterArray[0] = idValue;

            return new MethodQueryInfo(StatementType.UPDATE, query, returnType, parameterTypes, idParameterArray);
        }
    }






    public static MethodQueryInfoSupplier parse(String tableName, AnalyzedPropertyClass entityClass, DBEntityInfo entityInfo, Method method, AnalyzedPropertyClass returnType) throws PersistQueryBuildException{
//        MethodQueryInfo info = new MethodQueryInfo();
//        MethodQueryInfoSupplier infoSupplier = null;

        CustomQuery customQueryAnnotation = method.getAnnotation(CustomQuery.class);
        String customQuery = customQueryAnnotation == null ? null : customQueryAnnotation.value();
        return analyzeAndGenerateSqlFromMethodName(customQuery, tableName, entityClass, entityInfo, method.getName(), returnType);

//        if(customQueryAnnotation != null) info.query = customQueryAnnotation.value();

//        info.returnType = returnType;

//        return infoSupplier;
    }

    private static MethodQueryInfoSupplier analyzeAndGenerateSqlFromMethodName(String customQuery, String tableName, AnalyzedPropertyClass entityClass, DBEntityInfo entityInfo, String mName, AnalyzedPropertyClass returnType) throws PersistQueryBuildException{
        //GET / FIND
        for(String selectStart : SELECT_METHOD_NAME_VARIANTS){
            if(mName.startsWith(selectStart)){
                StaticMethodQueryInfoSupplier staticSupplier = new StaticMethodQueryInfoSupplier();

                staticSupplier.type = StatementType.QUERY;
                staticSupplier.returnType = returnType;

                StringBuilder sb = new StringBuilder();
                sb.append("SELECT ");
                addSelection(staticSupplier, entityClass, sb, tableName, mName, selectStart.length());
                staticSupplier.query = customQuery == null ? sb.toString() : customQuery;

                return staticSupplier;
            }
        }


        for(String updateStart : UPDATE_METHOD_NAME_VARIANTS){
            if(mName.startsWith(updateStart)){
                UpdateCreateMethodInfoSupplier updateCreateSupplier = new UpdateCreateMethodInfoSupplier(entityClass, entityInfo, returnType);

                StringBuilder sbCreate = new StringBuilder();
                sbCreate.append("INSERT INTO ").append(tableName).append("(");

                addCreate(updateCreateSupplier, entityClass, entityInfo, sbCreate, mName, updateStart.length());
                updateCreateSupplier.create_query = sbCreate.toString();

                StringBuilder sbUpdate = new StringBuilder();
                sbUpdate.append("UPDATE ").append(tableName).append(" SET ");

                addUpdate(updateCreateSupplier, entityClass, entityInfo, sbUpdate, mName, updateStart.length());
                updateCreateSupplier.update_query = sbUpdate.toString();

                return updateCreateSupplier;
            }
        }

        for(String deleteStart : DELETE_METHOD_NAME_VARIANTS){
            if(mName.startsWith(deleteStart)){
                DeleteMethodQueryInfoSupplier infoSupplier = new DeleteMethodQueryInfoSupplier(entityClass, entityInfo);
                infoSupplier.returnType = returnType;

                StringBuilder sb = new StringBuilder();
                sb.append("DELETE FROM ").append(tableName).append(" WHERE ");

                addDelete(infoSupplier, entityClass, entityInfo, sb, mName, deleteStart.length());

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

    private static void addSelection(StaticMethodQueryInfoSupplier supplier, AnalyzedPropertyClass entityClass, StringBuilder sb, String tableName, String s, int index) throws PersistQueryBuildException{
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

                    Class<?> type = getParameterTypeByColName(entityClass, colName);
                    if(type == null) throw new PersistQueryBuildException("Failed to get field of entity [" + entityClass.getType() + "] for column name [" + colName + "]!");
                    supplier.parameterTypes.add(type);
                }
                if(whereOrSplit.length > 1) sb.append(")");
            }
        }
    }

    private static void addCreate(UpdateCreateMethodInfoSupplier infoSupplier, AnalyzedPropertyClass entityClass, DBEntityInfo entityInfo, StringBuilder sb, String s, int index) throws PersistQueryBuildException{
        if(s.length() == index){
            Map<String, PropertyEntry> fieldEntries = entityClass.getFieldEntryMap();
//            String[] fieldNameOrder = new String[fieldEntries.size()];

            int i = 0;
            for(String fieldName : fieldEntries.keySet()){
                if(fieldName.equals(entityInfo.primaryKey) && entityInfo.primaryKeyGenerated) continue;

                if(i != 0) sb.append(",");
                sb.append(fieldName);

                PropertyEntry entry = fieldEntries.get(fieldName);
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

    private static void addUpdate(UpdateCreateMethodInfoSupplier infoSupplier, AnalyzedPropertyClass entityClass, DBEntityInfo entityInfo, StringBuilder sb, String s, int index) throws PersistQueryBuildException{
        if(s.length() == index){
            Map<String, PropertyEntry> fieldEntries = entityClass.getFieldEntryMap();

            int i = 0;
            for(String fieldName : fieldEntries.keySet()){
                if(fieldName.equals(entityInfo.primaryKey)) continue;

                if(i != 0) sb.append(", ");

                sb.append(fieldName).append(" = ?");

                PropertyEntry entry = fieldEntries.get(fieldName);
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

    private static void addDelete(DeleteMethodQueryInfoSupplier infoSupplier, AnalyzedPropertyClass entityClass, DBEntityInfo entityInfo, StringBuilder sb, String s, int index) throws PersistQueryBuildException{
        if(s.length() == index) {
            Map<String, PropertyEntry> fieldEntries = entityClass.getFieldEntryMap();
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
