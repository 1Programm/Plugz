package com.programm.plugz.persist.imbedded;

import com.programm.plugz.api.MagicRuntimeException;
import com.programm.plugz.cls.analyzer.AnalyzedParameterizedType;
import com.programm.plugz.cls.analyzer.AnalyzedPropertyClass;
import com.programm.plugz.cls.analyzer.PropertyEntry;
import com.programm.plugz.persist.*;
import com.programm.plugz.persist.ex.PersistQueryBuildException;
import com.programm.plugz.persist.ex.PersistRuntimeException;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DBHelper {

    public static String dbFieldName(String name){
        return name.toUpperCase();
    }

    public static String getPrimaryKey(AnalyzedPropertyClass cls){
        Map<String, PropertyEntry> fieldEntries = cls.getFieldEntryMap();

        for(String fieldName : fieldEntries.keySet()){
            PropertyEntry fieldEntry = fieldEntries.get(fieldName);
            if(fieldEntry.getField().isAnnotationPresent(ID.class)){
                return fieldName;
            }
        }

        return null;
    }

    public static String getTableNameFromEntity(PersistEntityInfo entityInfo) {
        Class<?> entityCls = entityInfo.analyzedEntity.getType();
        Entity entityAnnotation = entityCls.getAnnotation(Entity.class);
        if(entityAnnotation != null && !entityAnnotation.tableName().isEmpty()){
            return entityAnnotation.tableName().toUpperCase();
        }

        return entityCls.getSimpleName().toUpperCase();
    }

    public static String getForeignKeyFromInfo(String foreignTableName, PersistForeignKeyInfo info){
        return "FK_" + foreignTableName + "_" + dbFieldName(info.foreignKey);
    }

    public static boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        ResultSet resultSet = meta.getTables(null, null, tableName, new String[]{"TABLE"});
        return resultSet.next();
    }

    public static PersistEntityInfo createTableForAnalyzedEntity(Connection connection, String tableName, PersistEntityInfo entityInfo, Map<Class<?>, PersistEntityInfo> infoMap, Set<String> tableCheckExist) throws PersistQueryBuildException, SQLException {
        Map<String, PropertyEntry> fieldEntries = entityInfo.analyzedEntity.getFieldEntryMap();

        //Create table creation sql statement
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName).append(" (");

        List<String> foreignKeyNames = new ArrayList<>();
        List<PropertyEntry> foreignKeyFields = new ArrayList<>();
        List<String> foreignKeyIds = new ArrayList<>();

        for(String fieldName : fieldEntries.keySet()){
            PropertyEntry fieldEntry = fieldEntries.get(fieldName);
            Field field = fieldEntry.getField();


            PersistForeignKeyInfo foreignKeyInfo = entityInfo.foreignKeyInfoMap.get(fieldName);


            String dbFieldName = dbFieldName(fieldName);

            if(foreignKeyInfo != null){
                PersistEntityInfo foreignEntityInfo = infoMap.get(foreignKeyInfo.foreignEntityType);
                String foreignEntityTableName = getTableNameFromEntity(foreignEntityInfo);
                if(!tableCheckExist.contains(foreignEntityTableName)) return foreignEntityInfo;
                dbFieldName = getForeignKeyFromInfo(dbFieldName, foreignKeyInfo);
            }

            sb.append(dbFieldName);
            sb.append(" ");

//            AnalyzedPropertyClass analyzedFieldEntryType = fieldEntry.getPropertyType();
//            Class<?> fieldEntryType = analyzedFieldEntryType.getType();
//            PersistEntityInfo fieldEntityInfo = infoMap.get(fieldEntryType);

            boolean _notNull = false;
            boolean _nullable = false;


            if(fieldName.equals(entityInfo.primaryKey)){
                _notNull = true;
            }





            if(foreignKeyInfo == null){
                sb.append(getSqlDataTypeForFieldEntry(fieldEntry.getPropertyType()));
            }
            else {
//                foreignKeyInfo.

                if(foreignKeyInfo.connectionType == PersistForeignKeyInfo.ConnectionType.ONE_TO_ONE){
                    //Create an entry HERE for the id of the foreign entity, also put UNIQUE into statement
                    AnalyzedPropertyClass foreignEntityClass = fieldEntry.getPropertyType();
                    PropertyEntry foreignEntityKeyEntry = foreignEntityClass.getFieldEntryMap().get(foreignKeyInfo.foreignKey);

                    sb.append(getSqlDataTypeForFieldEntry(foreignEntityKeyEntry.getPropertyType()));
                    sb.append(" UNIQUE");
                }
                else if(foreignKeyInfo.connectionType == PersistForeignKeyInfo.ConnectionType.ONE_TO_MANY){
                    //Create an entry NOT HERE for the id of this primary key... so just skip
                }
                else if(foreignKeyInfo.connectionType == PersistForeignKeyInfo.ConnectionType.MANY_TO_ONE) {
                    //Create an entry HERE for the id of the foreign entity - no UNIQUE key here
                }
                else if(foreignKeyInfo.connectionType == PersistForeignKeyInfo.ConnectionType.MANY_TO_MANY){
                    //Create an ConnectionTable with the 2 primary keys
                }

                foreignKeyNames.add(dbFieldName);
                foreignKeyFields.add(fieldEntry);
                foreignKeyIds.add(foreignKeyInfo.foreignKey);
                _nullable = true;
            }

//            sb.append(getSqlDataTypeForFieldEntry(fieldEntry.getPropertyType()));

//            if(foreignKeyAnno != null){
//                foreignKeyNames.add(fieldName);
//                foreignKeyFields.add(fieldEntry);
//                _notNull = true;
//            }

            if(_notNull){
                sb.append(" NOT NULL");
            }
            else if(_nullable){
                sb.append(" NULL");
            }

            Generated isGeneratedByDb = field.getAnnotation(Generated.class);
            if(isGeneratedByDb != null){
                sb.append(" AUTO_INCREMENT");
            }

            sb.append(", ");
        }

        sb.append("PRIMARY KEY (").append(dbFieldName(entityInfo.primaryKey)).append(")");

        for(int i=0;i<foreignKeyFields.size();i++){
            String dbFieldName = foreignKeyNames.get(i);
            PropertyEntry fieldEntry = foreignKeyFields.get(i);
            sb.append(", FOREIGN KEY (").append(dbFieldName).append(") REFERENCES ");

            Class<?> foreignEntityType = fieldEntry.getType();
            PersistEntityInfo foreignEntityInfo = infoMap.get(foreignEntityType);
            String foreignTableName = getTableNameFromEntity(foreignEntityInfo);

            String foreignKeyId = dbFieldName(foreignKeyIds.get(i));
            sb.append(foreignTableName).append("(").append(foreignKeyId).append(")");
        }

//        FOREIGN KEY (PersonID) REFERENCES Persons(PersonID)
        sb.append(")");


        connection.createStatement().executeUpdate(sb.toString());

        return null;
    }

    private static String getSqlDataTypeForFieldEntry(AnalyzedPropertyClass fieldType) throws PersistQueryBuildException{
        Class<?> type = fieldType.getType();

        if(Boolean.class.equals(type) || Boolean.TYPE.equals(type)){
            return "BIT";
        }
        else if(String.class.equals(type)) {
            return "VARCHAR(100)";
        }
        else if(Byte.class.equals(type) || Byte.TYPE.equals(type)) {
            return "TINYINT";
        }
        else if(Short.class.equals(type) || Short.TYPE.equals(type)){
            return "SMALLINT";
        }
        else if(Integer.class.equals(type) || Integer.TYPE.equals(type)){
            return "INTEGER";
        }
        else if(Long.class.equals(type) || Long.TYPE.equals(type)){
            return "BIGINT";
        }
        else if(Float.class.equals(type) || Float.TYPE.equals(type)){
            return "REAL";
        }
        else if(Double.class.equals(type) || Double.TYPE.equals(type)){
            return "DOUBLE";
        }
        else if(type == Character.class || type == Character.TYPE){
            return "CHAR";
        }
        else if(Date.class.equals(type)){
            return "TIMESTAMP"; //It is "DATE" in Oracle databases ... and "TIMESTAMP" in all others ... bruh
        }

        throw new PersistQueryBuildException("Invalid data type [" + type + "]!");
    }

    public static void dropTable(Connection connection, String tableName) throws SQLException {
        connection.createStatement().executeUpdate("DROP TABLE " + tableName + " CASCADE");
    }

    public static void prepareStatement(PreparedStatement statement, int i, Class<?> type, Object value) throws SQLException{
        if(value == null){
            int dataType = getSqlDataTypeFromClass(type);
            statement.setNull(i, dataType);
        }
        else if(Boolean.class.equals(type) || Boolean.TYPE.equals(type)){
            statement.setBoolean(i, (boolean) value);
        }
        else if(String.class.equals(type)) {
            statement.setString(i, (String) value);
        }
        else if(Byte.class.equals(type) || Byte.TYPE.equals(type)) {
            statement.setByte(i, (byte) value);
        }
        else if(Short.class.equals(type) || Short.TYPE.equals(type)){
            statement.setShort(i, (short) value);
        }
        else if(Integer.class.equals(type) || Integer.TYPE.equals(type)){
            statement.setInt(i, (int) value);
        }
        else if(Long.class.equals(type) || Long.TYPE.equals(type)){
            statement.setLong(i, (long) value);
        }
        else if(Float.class.equals(type) || Float.TYPE.equals(type)){
            statement.setFloat(i, (float) value);
        }
        else if(Double.class.equals(type) || Double.TYPE.equals(type)){
            statement.setDouble(i, (double) value);
        }
        else if(type == Character.class || type == Character.TYPE){
            statement.setInt(i, (char) value);
        }
        else if(Date.class.equals(type)){
            statement.setDate(i, (Date) value);
        }
        else {
            throw new MagicRuntimeException("Unsupported parameter type for sql querry: [" + type + "]!");
        }
    }

    public static int getSqlDataTypeFromClass(Class<?> cls){
        if(Boolean.class.equals(cls) || Boolean.TYPE.equals(cls)){
            return Types.BOOLEAN;
        }
        else if(String.class.equals(cls)) {
            return Types.VARCHAR;
        }
        else if(Byte.class.equals(cls) || Byte.TYPE.equals(cls)) {
            return Types.TINYINT;
        }
        else if(Short.class.equals(cls) || Short.TYPE.equals(cls)){
            return Types.SMALLINT;
        }
        else if(Integer.class.equals(cls) || Integer.TYPE.equals(cls)){
            return Types.INTEGER;
        }
        else if(Long.class.equals(cls) || Long.TYPE.equals(cls)){
            return Types.BIGINT;
        }
        else if(Float.class.equals(cls) || Float.TYPE.equals(cls)){
            return Types.FLOAT;
        }
        else if(Double.class.equals(cls) || Double.TYPE.equals(cls)){
            return Types.DOUBLE;
        }
        else if(cls == Character.class || cls == Character.TYPE){
            return Types.CHAR;
        }
        else if(Date.class.equals(cls)){
            return Types.DATE;
        }
        else {
            throw new MagicRuntimeException("Unsupported parameter type for sql querry: [" + cls + "]!");
        }
    }

//    public static Object getResultSetDataFromType(ResultSet rs, String col, PropertyEntry prop) throws SQLException {
//        Class<?> type = prop.getType();
//
//        if(type == Boolean.class || type == Boolean.TYPE){
//            return rs.getBoolean(col);
//        }
//        else if(type == String.class){
//            return rs.getString(col);
//        }
//        else if(type == Byte.class || type == Byte.TYPE){
//            return rs.getByte(col);
//        }
//        else if(type == Short.class || type == Short.TYPE){
//            return rs.getShort(col);
//        }
//        else if(type == Integer.class || type == Integer.TYPE){
//            return rs.getInt(col);
//        }
//        else if(type == Long.class || type == Long.TYPE){
//            return rs.getLong(col);
//        }
//        else if(type == Float.class || type == Float.TYPE){
//            return rs.getFloat(col);
//        }
//        else if(type == Double.class || type == Double.TYPE){
//            return rs.getDouble(col);
//        }
//        else if(type == Character.class || type == Character.TYPE){
//            return rs.getInt(col);
//        }
//        else if(Date.class.equals(type)){
//            return rs.getDate(col);
//        }
//
//        throw new PersistRuntimeException("Unknown type for database [" + type + "]");
//    }

    public static Object getDataFromDBType(ResultSet rs, int col, int type) throws SQLException {
        if(type == Types.BIT){
            return rs.getBoolean(col);
        }
        else if(type == Types.VARCHAR){
            return rs.getString(col);
        }
        else if(type == Types.TINYINT){
            return rs.getByte(col);
        }
        else if(type == Types.SMALLINT){
            return rs.getShort(col);
        }
        else if(type == Types.INTEGER){
            return rs.getInt(col);
        }
        else if(type == Types.BIGINT){
            return rs.getLong(col);
        }
        else if(type == Types.FLOAT){
            return rs.getFloat(col);
        }
        else if(type == Types.DOUBLE){
            return rs.getDouble(col);
        }
        else if(type == Types.CHAR){
            return rs.getInt(col);
        }
        else if(type == Types.DATE){
            return rs.getDate(col);
        }

        throw new PersistRuntimeException("Invalid db type [" + type + "]");
    }



//    public static void updateEntity(Connection connection, String table, AnalyzedPropertyClass cls, DBEntityInfo entityInfo, Object data) throws SQLException, PersistException {
//        Map<String, PropertyEntry> fieldEntries = cls.getFieldEntryMap();
//        String primaryKey = entityInfo.primaryKey;
//        Object primaryKeyData;
//
//        try {
//            primaryKeyData = cls.getFieldEntryMap().get(primaryKey).getGetter().get(data);
//        }
//        catch (InvocationTargetException e){
//            throw new PersistException("Failed to get data of primary key!", e);
//        }
//
//        Statement s = connection.createStatement();
//
//        //Check if data is already persisted - if yes we need to update and not insert
//        ResultSet resultSet = s.executeQuery("SELECT " + primaryKey + " FROM " + table + " WHERE " + primaryKey + " = " + primaryKeyData);
//
//        StringBuilder keys = new StringBuilder();
//        StringBuilder values = new StringBuilder();
//        for(String fieldName : fieldEntries.keySet()){
//            PropertyEntry fieldEntry = fieldEntries.get(fieldName);
//            Object fieldEntryValue;
//            try {
//                fieldEntryValue = fieldEntry.getGetter().get(data);
//            }
//            catch (InvocationTargetException e){
//                throw new PersistException("Failed to call getter of field [" + fieldName + "]", e);
//            }
//
//            if(fieldName.equals(entityInfo.primaryKey) && entityInfo.primaryKeyGenerated) continue;
//
//            if(!keys.isEmpty()) keys.append(",");
//            keys.append(dbFieldName(fieldName));
//            if(!values.isEmpty()) values.append(",");
//            parseJavaObjectToStringDBObject(values, fieldEntryValue);
//        }
//
//
//        //Data exists
//        if(resultSet.next()){
//            s.executeUpdate("");
//        }
//        //Create New
//        else {
//            s.executeUpdate("INSERT INTO " + table + "(" + keys + ") VALUES(" + values + ")");
//        }
//    }

//    public static void parseJavaObjectToStringDBObject(StringBuilder sb, Object obj){
//        if(obj == null) {
//            sb.append("NULL");
//        }
//        else if(obj.getClass() == String.class) {
//            sb.append("'").append(obj).append("'");
//        }
//        else {
//            sb.append(obj);
//        }
//    }

}
