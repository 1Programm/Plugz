package com.programm.plugz.api.utils;

public class ValueUtils {

    public static Object getPrimitiveValue(String s){
        //Boolean
        if(s.equals("true")) return true;
        if(s.equals("false")) return false;

        //Numbers
        try{ return Byte.parseByte(s); } catch (NumberFormatException ignore){}
        try{ return Short.parseShort(s); } catch (NumberFormatException ignore){}
        try{ return Integer.parseInt(s); } catch (NumberFormatException ignore){}
        try{ return Long.parseLong(s); } catch (NumberFormatException ignore){}
        try{ return Float.parseFloat(s); } catch (NumberFormatException ignore){}
        try{ return Double.parseDouble(s); } catch (NumberFormatException ignore){}

        //Char
        if(s.length() == 1) return s.charAt(0);

        //String
        return s;
    }

}
