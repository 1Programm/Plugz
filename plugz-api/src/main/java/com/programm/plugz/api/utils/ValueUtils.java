package com.programm.plugz.api.utils;

import java.lang.reflect.Field;

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

    public static boolean isPrimitiveOrBoxed(Class<?> cls){
        return cls == Boolean.class || cls == Boolean.TYPE
                || cls == Byte.class || cls == Byte.TYPE
                || cls == Short.class || cls == Short.TYPE
                || cls == Integer.class || cls == Integer.TYPE
                || cls == Long.class || cls == Long.TYPE
                || cls == Float.class || cls == Float.TYPE
                || cls == Double.class || cls == Double.TYPE
                || cls == Character.class || cls == Character.TYPE;
    }

    public static Class<?> unwrapPrimitiveWrapper(Class<?> cls){
        if(cls.isPrimitive()) return cls;

        try {
            Field field = cls.getField("TYPE");
            return (Class<?>) field.get(null);
        }
        catch (NoSuchFieldException | IllegalAccessException ignore){}

        return cls;
    }

    @SuppressWarnings("unchecked")
    public static <T> T parsePrimitive(Object value, Class<T> cls) throws ValueParseException {
        if(cls == String.class) return value == null ? null : (T) value.toString();

        Class<?> pCls = unwrapPrimitiveWrapper(cls);

        if(!pCls.isPrimitive()) throw new ValueParseException("Not a primitive value: " + cls.getName());
        if(value == null) return (T)getDefaultValue(cls);

        Class<?> vCls = value.getClass();

        if(pCls == Boolean.TYPE){
            if(vCls == Boolean.class || vCls == Boolean.TYPE){
                return (T) value;
            }
            else {
                String _value = value.toString();
                if(_value.equalsIgnoreCase("false")){
                    return (T) Boolean.FALSE;
                }
                else if(_value.equalsIgnoreCase("true")){
                    return (T) Boolean.TRUE;
                }
                else {
                    throw new ValueParseException("Cannot parse [" + _value + "] to boolean!");
                }
            }
        }
        else if(pCls == Byte.TYPE){
            if(vCls == Byte.class || vCls == Byte.TYPE){
                return (T) value;
            }
            else if(value instanceof Number num){
                return (T) (Object) num.byteValue();
            }
            else {
                String _value = value.toString();
                try {
                    return (T) Byte.valueOf(_value);
                }
                catch (NumberFormatException e){
                    throw new ValueParseException("Could not parse value to byte!", e);
                }
            }
        }
        else if(pCls == Short.TYPE){
            if(vCls == Short.class || vCls == Short.TYPE){
                return (T) value;
            }
            else if(value instanceof Number num){
                return (T) (Object) num.shortValue();
            }
            else {
                String _value = value.toString();
                try {
                    return (T) Short.valueOf(_value);
                }
                catch (NumberFormatException e){
                    throw new ValueParseException("Could not parse value to short!", e);
                }
            }
        }
        else if(pCls == Integer.TYPE){
            if(vCls == Integer.class || vCls == Integer.TYPE){
                return (T) value;
            }
            else if(value instanceof Number num){
                return (T) (Object) num.intValue();
            }
            else {
                String _value = value.toString();
                try {
                    return (T) Integer.valueOf(_value);
                }
                catch (NumberFormatException e){
                    throw new ValueParseException("Could not parse value to int!", e);
                }
            }
        }
        else if(pCls == Long.TYPE){
            if(vCls == Long.class || vCls == Long.TYPE){
                return (T) value;
            }
            else if(value instanceof Number num){
                return (T) (Object) num.longValue();
            }
            else {
                String _value = value.toString();
                try {
                    return (T) Long.valueOf(_value);
                }
                catch (NumberFormatException e){
                    throw new ValueParseException("Could not parse value to long!", e);
                }
            }
        }
        else if(pCls == Float.TYPE){
            if(vCls == Float.class || vCls == Float.TYPE){
                return (T) value;
            }
            else if(value instanceof Number num){
                return (T) (Object) num.floatValue();
            }
            else {
                String _value = value.toString();
                try {
                    return (T) Float.valueOf(_value);
                }
                catch (NumberFormatException e){
                    throw new ValueParseException("Could not parse value to float!", e);
                }
            }
        }
        else if(pCls == Double.TYPE){
            if(vCls == Double.class || vCls == Double.TYPE){
                return (T) value;
            }
            else if(value instanceof Number num){
                return (T) (Object) num.doubleValue();
            }
            else {
                String _value = value.toString();
                try {
                    return (T) Double.valueOf(_value);
                }
                catch (NumberFormatException e){
                    throw new ValueParseException("Could not parse value to double!", e);
                }
            }
        }
        else if(pCls == Character.TYPE){
            if(vCls == Character.class || vCls == Character.TYPE){
                return (T) value;
            }
            else {
                String _value = value.toString();
                if(_value.length() != 1) throw new ValueParseException("Could not parse value [" + _value + "] to char!");
                return (T) (Object) _value.charAt(0);
            }
        }

        throw new IllegalStateException("INVALID STATE: There should be no other primitive values! [" + pCls + "]");
    }

}
