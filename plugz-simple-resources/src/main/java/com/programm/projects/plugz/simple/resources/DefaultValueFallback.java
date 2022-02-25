package com.programm.projects.plugz.simple.resources;

import com.programm.projects.plugz.magic.api.resources.IValueFallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultValueFallback implements IValueFallback {

    @Override
    public Object fallback(Class<?> expectedType, String resourceName, Object source) {
        if(CharSequence.class.isAssignableFrom(expectedType)){
            return "";
        }
        else if(Utils.isSameClass(expectedType, Boolean.class)){
            return false;
        }
        else if(Utils.isSameClass(expectedType, Character.class)){
            return (char)0;
        }
        else if(Utils.isSameClass(expectedType, Byte.class)){
            return (byte)0;
        }
        else if(Utils.isSameClass(expectedType, Short.class)){
            return (short)0;
        }
        else if(Utils.isSameClass(expectedType, Integer.class)){
            return 0;
        }
        else if(Utils.isSameClass(expectedType, Long.class)){
            return (long)0;
        }
        else if(Utils.isSameClass(expectedType, Float.class)){
            return (float)0;
        }
        else if(Utils.isSameClass(expectedType, Double.class)){
            return (double)0;
        }
        else if(List.class.isAssignableFrom(expectedType)){
            return new ArrayList<>();
        }
        else if(Map.class.isAssignableFrom(expectedType)){
            return new HashMap<>();
        }

        return null;
    }

}
