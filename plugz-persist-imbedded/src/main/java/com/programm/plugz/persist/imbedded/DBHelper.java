package com.programm.plugz.persist.imbedded;

import com.programm.plugz.api.MagicRuntimeException;
import com.programm.plugz.cls.analyzer.AnalyzedPropertyClass;
import com.programm.plugz.cls.analyzer.PropertyEntry;
import com.programm.plugz.persist.Generated;
import com.programm.plugz.persist.ID;
import com.programm.plugz.persist.ex.PersistException;
import com.programm.plugz.persist.ex.PersistQueryBuildException;
import com.programm.plugz.persist.ex.PersistRuntimeException;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.Map;

class DBHelper {

    public static void test(Connection connection){
        try {
            connection.createStatement().executeUpdate("INSERT INTO PERSON (NAME, AGE) VALUES('Julian', 23)");
            connection.createStatement().executeUpdate("INSERT INTO PERSON (NAME, AGE) VALUES('Felix', 20)");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String dbFieldName(String name){
        return name.toUpperCase();
    }

    public static DBEntityInfo createDBEntityInfo(AnalyzedPropertyClass cls){
        Map<String, PropertyEntry> fieldEntries = cls.getFieldEntryMap();

        String primaryKeyName = null;
        boolean primaryKeyGenerated = false;

        for(String fieldName : fieldEntries.keySet()){
            PropertyEntry fieldEntry = fieldEntries.get(fieldName);
            if(fieldEntry.getField().isAnnotationPresent(ID.class)){
                primaryKeyName = fieldName;
                if(fieldEntry.getField().isAnnotationPresent(Generated.class)){
                    primaryKeyGenerated = true;
                }
            }
        }

        return new DBEntityInfo(primaryKeyName, primaryKeyGenerated);
    }

    public static boolean tableNExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        ResultSet resultSet = meta.getTables(null, null, tableName, new String[]{"TABLE"});
        return !resultSet.next();
    }

    public static void createTableForAnalyzedEntity(Connection connection, String tableName, AnalyzedPropertyClass entity, DBEntityInfo entityInfo) throws PersistQueryBuildException, SQLException {
        Map<String, PropertyEntry> fieldEntries = entity.getFieldEntryMap();

        //Create table creation sql statement
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName).append(" (");

        for(String fieldName : fieldEntries.keySet()){
            PropertyEntry fieldEntry = fieldEntries.get(fieldName);
            sb.append(dbFieldName(fieldName));
            sb.append(" ");
            sb.append(getSqlDataTypeForFieldEntry(fieldEntry.getPropertyType()));

            if(fieldName.equals(entityInfo.primaryKey)){
                sb.append(" NOT NULL");
                if(entityInfo.primaryKeyGenerated) {
                    sb.append(" AUTO_INCREMENT");
                }
            }

            sb.append(", ");
        }

        sb.append("PRIMARY KEY (").append(dbFieldName(entityInfo.primaryKey)).append(")");
        sb.append(")");


        connection.createStatement().executeUpdate(sb.toString());
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
        connection.createStatement().executeUpdate("DROP TABLE " + tableName);
    }

    public static void prepareStatement(PreparedStatement statement, int i, Class<?> type, Object value) throws SQLException{
        if(Boolean.class.equals(type) || Boolean.TYPE.equals(type)){
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

    public static Object getResultSetDataFromType(ResultSet rs, String col, PropertyEntry prop) throws SQLException {
        Class<?> type = prop.getType();

        if(type == Boolean.class || type == Boolean.TYPE){
            return rs.getBoolean(col);
        }
        else if(type == String.class){
            return rs.getString(col);
        }
        else if(type == Byte.class || type == Byte.TYPE){
            return rs.getByte(col);
        }
        else if(type == Short.class || type == Short.TYPE){
            return rs.getShort(col);
        }
        else if(type == Integer.class || type == Integer.TYPE){
            return rs.getInt(col);
        }
        else if(type == Long.class || type == Long.TYPE){
            return rs.getLong(col);
        }
        else if(type == Float.class || type == Float.TYPE){
            return rs.getFloat(col);
        }
        else if(type == Double.class || type == Double.TYPE){
            return rs.getDouble(col);
        }
        else if(type == Character.class || type == Character.TYPE){
            return rs.getInt(col);
        }
        else if(Date.class.equals(type)){
            return rs.getDate(col);
        }

        throw new PersistRuntimeException("Unknown type for database [" + type + "]");
    }

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
        else if(type == Types.REAL){
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



    public static void updateEntity(Connection connection, String table, AnalyzedPropertyClass cls, DBEntityInfo entityInfo, Object data) throws SQLException, PersistException {
        Map<String, PropertyEntry> fieldEntries = cls.getFieldEntryMap();
        String primaryKey = entityInfo.primaryKey;
        Object primaryKeyData;

        try {
            primaryKeyData = cls.getFieldEntryMap().get(primaryKey).getGetter().get(data);
        }
        catch (InvocationTargetException e){
            throw new PersistException("Failed to get data of primary key!", e);
        }

        Statement s = connection.createStatement();

        //Check if data is already persisted - if yes we need to update and not insert
        ResultSet resultSet = s.executeQuery("SELECT " + primaryKey + " FROM " + table + " WHERE " + primaryKey + " = " + primaryKeyData);

        StringBuilder keys = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for(String fieldName : fieldEntries.keySet()){
            PropertyEntry fieldEntry = fieldEntries.get(fieldName);
            Object fieldEntryValue;
            try {
                fieldEntryValue = fieldEntry.getGetter().get(data);
            }
            catch (InvocationTargetException e){
                throw new PersistException("Failed to call getter of field [" + fieldName + "]", e);
            }

            if(fieldName.equals(entityInfo.primaryKey) && entityInfo.primaryKeyGenerated) continue;

            if(!keys.isEmpty()) keys.append(",");
            keys.append(dbFieldName(fieldName));
            if(!values.isEmpty()) values.append(",");
            parseJavaObjectToStringDBObject(values, fieldEntryValue);
        }


        //Data exists
        if(resultSet.next()){
            s.executeUpdate("");
        }
        //Create New
        else {
            s.executeUpdate("INSERT INTO " + table + "(" + keys + ") VALUES(" + values + ")");
        }
    }

    public static void parseJavaObjectToStringDBObject(StringBuilder sb, Object obj){
        if(obj == null) {
            sb.append("NULL");
        }
        else if(obj.getClass() == String.class) {
            sb.append("'").append(obj).append("'");
        }
        else {
            sb.append(obj);
        }
    }

}
