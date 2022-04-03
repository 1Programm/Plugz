package com.programm.projects.plugz.simple.webserv;

import com.programm.projects.plugz.simple.webserv.entityutil.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class JsonContentWriter implements IContentWriter{

    @Override
    public String write(Object value) throws ContentMapException {
        Class<?> cls = value.getClass();

//        if(cls == String.class){
//            return "\"" + parseJson(value, cls) + "\"";
//        }

        return parseJson(value, cls);
    }

    private String parseJson(Object value, Class<?> cls) throws ContentMapException {
        String ret = tryPrimitive(value, cls);
        if(ret != null) return ret;

        EntityEntry entityEntry;
        try {
            entityEntry = EntityMapper.createEntry(cls);
        }
        catch (EntityMapException e){
            throw new ContentMapException("Could not create entry for entity: [" + cls.getName() + "]!", e);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");

        for(Map.Entry<String, FieldEntry> entry : entityEntry.getFieldEntryMap().entrySet()){
            FieldEntry fieldEntry = entry.getValue();
            String fieldName = fieldEntry.getField().getName();

            IFieldGetter getter = fieldEntry.getGetter();

            if(getter == null) continue;

            if(sb.length() != 1) sb.append(",");
            sb.append("\"").append(fieldName).append("\":");

            Object data;
            try {
                data = getter.get(value);
            }
            catch (InvocationTargetException e){
                throw new ContentMapException("Exception when calling getter method for entity entry: [" + cls.getName() + "].", e);
            }

            String childJson = parseJson(data, data.getClass());
            sb.append(childJson);
        }

        sb.append("}");

        return sb.toString();
    }

    private String tryPrimitive(Object value, Class<?> cls) throws ContentMapException {
        if(cls == String.class){
            return "\"" + escapeEscapeSequences(value.toString()) + "\"";
        }
        else if(ClassUtils.isPrimitiveOrBoxed(cls)){
            return value.toString();
        }
        else if(cls.isArray()){
            Object[] arr = (Object[])value;
            StringBuilder sb = new StringBuilder();
            sb.append("[");

            for(int i=0;i<arr.length;i++){
                if(i != 0) sb.append(",");
                Object obj = arr[i];
                String _obj = parseJson(obj, obj.getClass());
                sb.append(_obj);
            }

            sb.append("]");
            return sb.toString();
        }
        else if(List.class.isAssignableFrom(cls)){
            List<?> list = (List<?>) value;
            StringBuilder sb = new StringBuilder();
            sb.append("[");

            for(int i=0;i<list.size();i++){
                if(i != 0) sb.append(",");
                Object obj = list.get(i);
                String _obj = parseJson(obj, obj.getClass());
                sb.append(_obj);
            }

            sb.append("]");
            return sb.toString();
        }
        else if(Map.class.isAssignableFrom(cls)){
            Map<?, ?> map = (Map<?, ?>) value;
            StringBuilder sb = new StringBuilder();
            sb.append("{");

            for(Object key : map.keySet()){
                String _key = escapeEscapeSequences(key.toString());
                Object val = map.get(key);
                String _val = parseJson(val, val.getClass());

                if(sb.length() != 1) sb.append(",");
                sb.append("\"").append(_key).append("\":\"").append(_val).append("\"");
            }

            sb.append("}");
            return sb.toString();
        }

        return null;
    }

    private String escapeEscapeSequences(String s){
        StringBuilder sb = new StringBuilder();

        int last = 0;
        int sLen = s.length();
        for(int i=0;i<sLen;i++){
            char c = s.charAt(i);

            if(c == '\t'){
                sb.append(s, last, i);
                sb.append("\\t");
                last = i + 1;
            }
            else if(c == '\b'){
                sb.append(s, last, i);
                sb.append("\\b");
                last = i + 1;
            }
            else if(c == '\n'){
                sb.append(s, last, i);
                sb.append("\\n");
                last = i + 1;
            }
            else if(c == '\r'){
                sb.append(s, last, i);
                sb.append("\\r");
                last = i + 1;
            }
            else if(c == '\f'){
                sb.append(s, last, i);
                sb.append("\\f");
                last = i + 1;
            }
//            else if(c == '\''){
//                sb.append(s, last, i);
//                sb.append("'");
//                last = i + 1;
//            }
            else if(c == '\"'){
                sb.append(s, last, i);
                sb.append("\\\"");
                last = i + 1;
            }
//            else if(c == '\\'){
//                sb.append(s, last, i);
//                sb.append("\\");
//                last = i + 1;
//            }
        }

        if(last == 0) return s;

        if(last < sLen){
            sb.append(s, last, sLen);
        }

        return sb.toString();
    }
}
