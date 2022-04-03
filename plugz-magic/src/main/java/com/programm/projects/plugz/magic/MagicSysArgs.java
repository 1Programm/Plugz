package com.programm.projects.plugz.magic;

import com.programm.projects.plugz.magic.api.SysArgs;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
class MagicSysArgs implements SysArgs {

    public static MagicSysArgs parseArgs(String[] args){
        Map<String, Object> map = new HashMap<>();

        String key = null;

        for (String arg : args) {
            if (arg.startsWith("-")) {
                if (key != null) {
                    map.put(key, true);
                }
                key = arg;
            } else {
                if (key != null) {
                    Object val = parseVal(arg);
                    map.put(key, val);
                    key = null;
                } else {
                    map.put(arg, true);
                }
            }
        }

        if(key != null){
            map.put(key, true);
        }

        return new MagicSysArgs(args, map);
    }

    private static Object parseVal(String s){
        if(s.equalsIgnoreCase("true")) return true;
        if(s.equalsIgnoreCase("false")) return false;

        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException ignore){}

        try {
            return Float.parseFloat(s);
        }
        catch (NumberFormatException ignore){}

        try {
            return Double.parseDouble(s);
        }
        catch (NumberFormatException ignore){}

        return s;
    }

    private final String[] originalArgs;
    private final Map<String, Object> args;

    @Override
    public Object get(String name) {
        return args.get(name);
    }

    @Override
    public <T> T get(String name, Class<T> cls) {
        return cls.cast(args.get(name));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getDefault(String name, T defaultValue) {
        T val = (T) get(name, defaultValue.getClass());
        return val != null ? val : defaultValue;
    }

    @Override
    public String[] getOriginal() {
        return originalArgs;
    }
}
