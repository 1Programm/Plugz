package com.programm.projects.plugz.simple.resources;

import java.net.URL;

class Utils {

    public static boolean isSameClass(Class<?> a, Class<?> b){
        if(a == b) return true;

        if(a.isPrimitive()){
            return a == toPrimitive(b);
        }

        if(b.isPrimitive()){
            return b == toPrimitive(a);
        }

        return false;
    }

    private static Class<?> toPrimitive(Class<?> c){
        try {
            return (Class<?>) c.getField("TYPE").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

    public static URL getUrlFromClass(Class<?> cls){
        return cls.getProtectionDomain().getCodeSource().getLocation();
    }

    public static String allToDots(String s){
        StringBuilder sb = new StringBuilder();

        boolean flag = false;

        for(int i=0;i<s.length();i++){
            char c = s.charAt(i);

            if(Character.isUpperCase(c)){
                if(flag){
                    sb.append(".").append(Character.toLowerCase(c));
                    flag = false;
                }
                else {
                    sb.append(Character.toLowerCase(c));
                }
            }
            else {
                flag = true;

                if (c == '_') {
                    sb.append(".");
                } else {
                    sb.append(c);
                }
            }
        }

        return sb.toString();
    }

    public static String camelCaseToDotsUpper(String s){
        return camelCaseToDots(s).toUpperCase();
    }

    public static String camelCaseToDots(String s){
        StringBuilder sb = new StringBuilder();

        for(int i=0;i<s.length();i++){
            char c = s.charAt(i);

            if(Character.isUpperCase(c)){
                sb.append(".").append(Character.toLowerCase(c));
            }
            else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    public static String camelCaseToUnderscoreUpper(String s){
        return camelCaseToUnderscore(s).toUpperCase();
    }

    public static String camelCaseToUnderscore(String s){
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

    public static String underscoreToDotsUpper(String s){
        return underscoreToDots(s).toUpperCase();
    }

    public static String underscoreToDots(String s){
        return s.replaceAll("_", ".");
    }

}