package com.programm.projects.plugz.db.abstractbase;

public class DBBaseUtils {

    public static boolean isNotSameClass(Class<?> a, Class<?> b){
        if(a == b) return false;

        if(a.isPrimitive()){
            return a != toPrimitive(b);
        }

        if(b.isPrimitive()){
            return b != toPrimitive(a);
        }

        return true;
    }

    private static Class<?> toPrimitive(Class<?> c){
        try {
            return (Class<?>) c.getField("TYPE").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }


    /**
     * getById -> get_by_id
     */
    public static String nameToStd(String s){
        StringBuilder sb = new StringBuilder();

        for(int i=0;i<s.length();i++){
            char c = s.charAt(i);

            if(Character.isUpperCase(c)){
                sb.append("_").append(Character.toLowerCase(c));
            }
            else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

}
