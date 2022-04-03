package com.programm.projects.plugz.simple.webserv;

import com.programm.projects.plugz.simple.webserv.entityutil.*;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonContentReader implements IContentReader {

    private static class Index {
        int i;

        @Override
        public String toString() {
            return "" + i;
        }
    }

    private final Map<Class<?>, EntityEntry> cachedEntries = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(String content, Class<T> cls) throws ContentMapException {
        return (T) parseJson(content, new Index(), cls);
    }

    private Object parseJson(String content, Index index, Class<?> cls) throws ContentMapException {
        char contentC = content.charAt(index.i);
        if (contentC == '{') {
            testObject(cls);

            EntityEntry entityEntry;
            try {
                entityEntry = getEntry(cls);
            } catch (EntityMapException e) {
                throw new ContentMapException("Could not create entry for entity: [" + cls.getName() + "]!", e);
            }

            Object instance = createInstance(cls);

            while(index.i < content.length()) {
                index.i++;
                while (Character.isWhitespace(content.charAt(index.i))) index.i++;
                if (content.charAt(index.i) != '\"') throw new ContentMapException("Key must be surrounded by quotation marks.");

                index.i++;
                String key = readStringValue(content, index);
                if (key == null) throw new ContentMapException("Key must be surrounded by quotation marks.");
                key = EntityUtils.nameToStd(key);

                FieldEntry assignedFieldEntry = entityEntry.getFieldEntryMap().get(key);
                index.i++;
                while (Character.isWhitespace(content.charAt(index.i))) index.i++;
                if (content.charAt(index.i) != ':') throw new ContentMapException("Key-Value pairs must be separated by a colon.");

                index.i++;
                while (Character.isWhitespace(content.charAt(index.i))) index.i++;

                Class<?> assignedFieldType = assignedFieldEntry == null ? null : assignedFieldEntry.getType();
                Object o = parseJson(content, index, assignedFieldType);

                index.i++;
                if (assignedFieldType != null) {
                    IFieldSetter setter = assignedFieldEntry.getSetter();
                    if(setter == null) throw new ContentMapException("Cannot set value as no setter is available!");

                    try {
                        setter.set(instance, o);
                    } catch (InvocationTargetException e) {
                        throw new ContentMapException("Exception when invoking setter.", e);
                    }
                }

                if(content.charAt(index.i) == '}') break;
                if(content.charAt(index.i) != ',') throw new ContentMapException("Values in an object must be separated by a comma.");
            }

            return instance;
        }
        else if(contentC == '[') {
            if(cls.isArray()){
                return parseArray(content, index, cls.getComponentType());
            }
            else if(List.class.isAssignableFrom(cls)){
                throw new ContentMapException("List and generic data types are not allowed!");
//                List<?> list;
//
//                if(cls == List.class){
//                    list = new ArrayList<>();
//                }
//                else {
//                    try {
//                        list = (List<?>) cls.getConstructor().newInstance();
//                    }
//                    catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e){
//                        throw new ContentMapException("Could not create intance of expected List-Type: [" + cls.getName() + "].", e);
//                    }
//                }
//
//                parseList(content, index, list);
//                return list;
            }
            else {
                throw new ContentMapException("Content is an array - expected type: " + cls.getName());
            }
        }
        else if (contentC == '"') {
            index.i++;
            String val = readStringValue(content, index);

            if (val == null) throw new ContentMapException("String value must be surrounded by quotation marks.");
            if (cls == null) return null;
            if (cls != String.class) throw new ContentMapException("Invalid type for string value: [" + cls.getName() + "]!");

            return val;
        }
        else {
            StringBuilder _content = new StringBuilder();
            _content.append(contentC);

            for(index.i++;index.i<content.length();index.i++){
                char c = content.charAt(index.i);

                if(c == ',' || c == '}') break;
                _content.append(c);
            }
            index.i--;

            String nContent = _content.toString();

            try {
                return Integer.valueOf(nContent);
            }
            catch (NumberFormatException ignore){}

            try {
                return Float.valueOf(nContent);
            }
            catch (NumberFormatException ignore){}

            try {
                return Double.valueOf(nContent);
            }
            catch (NumberFormatException ignore){}

            if(nContent.equalsIgnoreCase("true")) return true;
            if(nContent.equalsIgnoreCase("false")) return false;

            throw new ContentMapException("Invalid data: [" + nContent + "]!");
        }
    }

    private Object[] parseArray(String content, Index index, Class<?> elementType) throws ContentMapException{
        List<Object> arrList = new ArrayList<>();

        while(index.i < content.length()) {
            index.i++;
            while (Character.isWhitespace(content.charAt(index.i))) index.i++;

            if(content.charAt(index.i) == ']'){
                index.i++;
                break;
            }
            else {
                Object data = parseJson(content, index, elementType);
                arrList.add(data);

                index.i++;
                while (Character.isWhitespace(content.charAt(index.i))) index.i++;
                if(content.charAt(index.i) == ']') break;
                if(content.charAt(index.i) == ',') continue;
                throw new ContentMapException("Unexpected character in array: [" + content.charAt(index.i) + "]!");
            }
        }

        Object[] arr = (Object[]) Array.newInstance(elementType, arrList.size());
        return arrList.toArray(arr);
    }

    private void parseList(String content, Index index, List<?> l) throws ContentMapException {
//        while(index.i < content.length()) {
//            index.i++;
//            while (Character.isWhitespace(content.charAt(index.i))) index.i++;
//
//            if(content.charAt(index.i) == ']'){
//                index.i++;
//                break;
//            }
//            else {
//                Object data = parseJson(content, index, elementType);
//                arrList.add(data);
//
//                index.i++;
//                while (Character.isWhitespace(content.charAt(index.i))) index.i++;
//                if(content.charAt(index.i) == ']') break;
//                if(content.charAt(index.i) == ',') continue;
//                throw new ContentMapException("Unexpected character in array: [" + content.charAt(index.i) + "]!");
//            }
//        }
    }



    private String readStringValue(String content, Index index){
        boolean escape = false;
        StringBuilder sb = new StringBuilder();

        for(;index.i<content.length();index.i++){
            char c = content.charAt(index.i);
            if(escape){
                if(c == 'n'){       sb.append("\n"); }
                else if(c == 'r'){  sb.append("\r"); }
                else if(c == 't'){  sb.append("\t"); }
                else if(c == '\\'){ sb.append("\\"); }
                else if(c == '"'){  sb.append("\""); }
                else {
                    System.err.println("INVALID ESCAPED TOKEN: " + c);
                }
                escape = false;
            }
            else {
                if (c == '\\') {
                    escape = true;
                }
                else if(c == '"'){
                    break;
                }
                else if(index.i + 1 == content.length()){
                    return null;
                }
                else {
                    sb.append(c);
                }
            }
        }

        return sb.toString();
    }

    private Object createInstance(Class<?> cls) throws ContentMapException {
        Constructor<?> constructor;

        try {
            constructor = cls.getDeclaredConstructor();
        }
        catch (NoSuchMethodException e){
            throw new ContentMapException("No empty constructor available!");
        }

        try {
            boolean access = constructor.canAccess(null);

            if(!access) constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            if(!access) constructor.setAccessible(false);

            return instance;
        }
        catch (IllegalAccessException e){
            throw new ContentMapException("Empty constructor is private!", e);
        }
        catch (InstantiationException e){
            throw new ContentMapException("Could not instantiate class: " + cls.getName(), e);
        }
        catch (InvocationTargetException e){
            throw new ContentMapException("Exception when invoking empty constructor!", e);
        }
    }

    private void testObject(Class<?> cls) throws ContentMapException {
        if(cls == String.class
                || ClassUtils.isPrimitiveOrBoxed(cls)
                || cls.isArray()
                || List.class.isAssignableFrom(cls)
                || Map.class.isAssignableFrom(cls)) {
            throw new ContentMapException("Content is an object - expected type: " + cls.getName());
        }
    }

    private void testArray(Class<?> cls) throws ContentMapException {
        if(!(cls.isArray() || List.class.isAssignableFrom(cls))){
            throw new ContentMapException("Content is an array - expected type: " + cls.getName());
        }
    }

    private EntityEntry getEntry(Class<?> cls) throws EntityMapException {
        EntityEntry entry = cachedEntries.get(cls);

        if(entry == null){
            entry = EntityMapper.createEntry(cls);
        }

        return entry;
    }


}
