package com.programm.plugz.object.mapper.property;

import com.programm.plugz.cls.analyzer.*;
import com.programm.plugz.files.json.JsonArrayNode;
import com.programm.plugz.files.json.JsonNode;
import com.programm.plugz.files.json.JsonObjectNode;
import com.programm.plugz.files.json.JsonValueNode;
import com.programm.plugz.object.mapper.IConfigurableObjectWriter;
import com.programm.plugz.object.mapper.IObjectWriter;
import com.programm.plugz.object.mapper.ObjectMapException;
import com.programm.plugz.object.mapper.utils.ValueUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class JsonPropertyWriter implements IConfigurableObjectWriter<JsonNode, Object> {

    private final ClassAnalyzer analyzer;
    private final Map<Class<?>, IObjectWriter<JsonNode, ?>> writers = new HashMap<>();

    public JsonPropertyWriter() {
        this(new ClassAnalyzer(true));
    }

    public JsonPropertyWriter(ClassAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public JsonNode write(Object entity) throws ObjectMapException {
        return write(entity, Collections.emptyMap());
    }

    public JsonNode write(Object entity, Map<String, AnalyzedParameterizedType> parameterizedTypes) throws ObjectMapException {
        if(entity == null) throw new ObjectMapException("Cannot map null elements!");

        Class<?> cls = entity.getClass();
        IObjectWriter<JsonNode, ?> writer = writers.get(cls);

        if(writer != null){
            return writer._write(entity);
        }

        if(cls == String.class || ValueUtils.isPrimitiveOrBoxed(cls)){
            return new JsonValueNode(entity);
        }
        else if(cls.isArray()){
            Object[] arrEntity = (Object[]) entity;
            List<JsonNode> arrList = new ArrayList<>();

            for(Object e : arrEntity){
                if(e == null) continue;
                arrList.add(write(e));
            }

            return new JsonArrayNode(arrList);
        }
        else if(List.class.isAssignableFrom(cls)){
            List<?> arrEntity = (List<?>) entity;
            List<JsonNode> arrList = new ArrayList<>();

            for(Object e : arrEntity){
                if(e == null) continue;
                arrList.add(write(e));
            }

            return new JsonArrayNode(arrList);
        }
        else if(Set.class.isAssignableFrom(cls)){
            Set<?> arrEntity = (Set<?>) entity;
            List<JsonNode> arrList = new ArrayList<>();

            for(Object e : arrEntity){
                if(e == null) continue;
                arrList.add(write(e));
            }

            return new JsonArrayNode(arrList);
        }
        else if(Map.class.isAssignableFrom(cls)){
            Map<?, ?> mapEntity = (Map<?, ?>) entity;
            Map<String, JsonNode> theMap = new HashMap<>();

            for(Map.Entry<?, ?> entry : mapEntity.entrySet()){
                String key = entry.getKey().toString();
                Object value = entry.getValue();
                if(value == null) continue;
                JsonNode valueNode = write(value);
                theMap.put(key, valueNode);
            }

            return new JsonObjectNode(theMap);
        }
        else {
            AnalyzedPropertyClass analyzedClass;
            try {
                analyzedClass = analyzer.analyzeProperty(cls, parameterizedTypes);
            } catch (ClassAnalyzeException e) {
                throw new ObjectMapException("Failed to analyze class [" + cls.getName() + "]!", e);
            }

            Map<String, JsonNode> theObject = new HashMap<>();

            for(Map.Entry<String, PropertyEntry> entry : analyzedClass.getFieldEntryMap().entrySet()){
                PropertyEntry entryValue = entry.getValue();
                IClassPropertyGetter getter = entryValue.getGetter();
                if(getter == null) continue;
                try {
                    Object entryValueInstance = getter.get(entity);
                    if(entryValueInstance == null) continue;
                    JsonNode entryValueNode = write(entryValueInstance);
                    theObject.put(entry.getKey(), entryValueNode);
                }
                catch (InvocationTargetException e){
                    throw new ObjectMapException("Failed to get property from getter.", e);
                }
            }

            return new JsonObjectNode(theObject);
        }
    }

    @Override
    public JsonPropertyWriter registerWriter(Class<?> cls, IObjectWriter<JsonNode, Object> writer){
        writers.put(cls, writer);
        return this;
    }
}
