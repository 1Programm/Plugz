package com.programm.plugz.persist.imbedded;

import com.programm.plugz.cls.analyzer.AnalyzedPropertyClass;
import com.programm.plugz.cls.analyzer.PropertyEntry;
import com.programm.plugz.persist.CustomQuery;

import java.lang.reflect.Method;
import java.util.Map;

class MethodQueryParser {

    public static MethodQueryInfo parse(String tableName, AnalyzedPropertyClass entityClass, DBEntityInfo entityInfo, Method method, AnalyzedPropertyClass returnType){
        CustomQuery customQueryAnnotation = method.getAnnotation(CustomQuery.class);
        String query;

        if(customQueryAnnotation != null){
            query = customQueryAnnotation.value();
        }
        else {
            query = generateSqlStatementFromMethodName(tableName, entityClass, entityInfo, method.getName());
        }

        Class<?>[] parameterTypes = method.getParameterTypes();

        return new MethodQueryInfo(query, returnType, parameterTypes);
    }

    private static String generateSqlStatementFromMethodName(String tableName, AnalyzedPropertyClass entityClass, DBEntityInfo entityInfo, String mName){
        StringBuilder sb = new StringBuilder();

        //GET / FIND
        if(mName.startsWith("findAll")){
            sb.append("SELECT ");
            addSelection(sb, tableName, mName, "findAll".length());
        }
        else if(mName.startsWith("find")){
            sb.append("SELECT ");
            addSelection(sb, tableName, mName, "find".length());
        }
        else if(mName.startsWith("getAll")){
            sb.append("SELECT ");
            addSelection(sb, tableName, mName, "getAll".length());
        }
        else if(mName.startsWith("get")){
            sb.append("SELECT ");
            addSelection(sb, tableName, mName, "get".length());
        }

        //UPDATE
        else if(mName.startsWith("update")){
            sb.append("UPDATE ").append(tableName).append(" SET ");
            addUpdate(sb, mName, "update".length(), entityClass, entityInfo);
        }

        return sb.toString();
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

    private static void addSelection(StringBuilder sb, String tableName, String s, int index){
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
                }
                if(whereOrSplit.length > 1) sb.append(")");
            }
        }
    }

    private static void addUpdate(StringBuilder sb, String s, int index, AnalyzedPropertyClass entityClass, DBEntityInfo entityInfo){
        if(s.length() == index){
            Map<String, PropertyEntry> fieldEntries = entityClass.getFieldEntryMap();
            boolean init = true;
            for(String fieldName : fieldEntries.keySet()){
                if(fieldName.equals(entityInfo.primaryKey)) continue;

                if(init) init = false;
                else sb.append(", ");

                sb.append(fieldName).append(" = ?");
            }

            sb.append(" WHERE ").append(entityInfo.primaryKey).append(" = ?");
        }
    }

    private static void addByStatement(StringBuilder sb, String s, int index){
        String rest = s.substring(index);
        System.out.println(rest);
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
