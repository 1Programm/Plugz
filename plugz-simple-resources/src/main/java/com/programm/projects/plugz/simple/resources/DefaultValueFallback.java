package com.programm.projects.plugz.simple.resources;

import com.programm.projects.plugz.magic.api.resources.IValueFallback;

public class DefaultValueFallback implements IValueFallback {

    @Override
    public Object fallback(Class<?> expectedType, String resourceName, Object source) {
        if(expectedType == Boolean.TYPE){
            return false;
        }
        else if(expectedType == Character.TYPE){
            return (char)0;
        }
        else if(expectedType == Byte.TYPE){
            return (byte)0;
        }
        else if(expectedType == Short.TYPE){
            return (short)0;
        }
        else if(expectedType == Integer.TYPE){
            return 0;
        }
        else if(expectedType == Long.TYPE){
            return (long)0;
        }
        else if(expectedType == Float.TYPE){
            return (float)0;
        }
        else if(expectedType == Double.TYPE){
            return (double)0;
        }

        return "";
    }

}
