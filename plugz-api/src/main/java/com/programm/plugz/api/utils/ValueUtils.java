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

    public static Object getDefaultValue(Class<?> cls){
        if(!cls.isPrimitive()) return null;

        if(cls == Boolean.TYPE)     return false;
        if(cls == Byte.TYPE)        return (byte)0;
        if(cls == Character.TYPE)   return (char)0;
        if(cls == Short.TYPE)       return (short)0;
        if(cls == Integer.TYPE)     return 0;
        if(cls == Long.TYPE)        return (long)0;
        if(cls == Float.TYPE)       return (float)0;
        if(cls == Double.TYPE)      return (double)0;

        throw new IllegalStateException("INVALID STATE: There should be no other primitive values! [" + cls + "]");
    }

}
