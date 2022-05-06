package com.programm.plugz.object.mapper.property;

import com.programm.plugz.cls.analyzer.*;
import com.programm.plugz.files.json.JsonArrayNode;
import com.programm.plugz.files.json.JsonNode;
import com.programm.plugz.files.json.JsonObjectNode;
import com.programm.plugz.files.json.JsonValueNode;
import com.programm.plugz.object.mapper.IObjectMapper;
import com.programm.plugz.object.mapper.ISpecializedObjectMapperLookup;
import com.programm.plugz.object.mapper.ObjectMapException;
import com.programm.plugz.object.mapper.utils.ValueUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

public class JsonNodePropertyObjectMapper implements IObjectMapper<JsonNode, Object> {

    private final ClassAnalyzer analyzer;
    private final ISpecializedObjectMapperLookup specializedLookup;

    public JsonNodePropertyObjectMapper(ISpecializedObjectMapperLookup specializedLookup) {
        this(new ClassAnalyzer(true, false), specializedLookup);
    }

    public JsonNodePropertyObjectMapper(ClassAnalyzer analyzer, ISpecializedObjectMapperLookup specializedLookup) {
        this.analyzer = analyzer;
        this.specializedLookup = specializedLookup;
    }

    @Override
    public Object read(JsonNode node, Class<?> cls) throws ObjectMapException {
        return read(node, cls, Collections.emptyMap());
    }

    public Object read(JsonNode node, Class<?> cls, Map<String, AnalyzedParameterizedType> parameterizedTypes) throws ObjectMapException {
        if(specializedLookup != null) {
            IObjectMapper<JsonNode, ?> reader = specializedLookup.get(JsonNode.class, cls);
            if (reader != null) return reader._read(node, cls);
        }

        if(ValueUtils.isPrimitiveOrBoxed(cls)){
            if(node instanceof JsonValueNode valueNode) {
                return ValueUtils.parsePrimitive(valueNode.get(), cls);
            }

            throw new ObjectMapException("Content [" + node + "] cannot be parsed to primitive value [" + cls.getName() + "]!");
        }
        else if(cls == String.class){
            if(node instanceof JsonValueNode valueNode) {
                return valueNode.value();
            }

            throw new ObjectMapException("Content [" + node + "] cannot be parsed to String!");
        }
        else if(cls.isArray()){
            if(node instanceof JsonArrayNode arrayNode){
                int num = arrayNode.size();
                Class<?> arrayType = cls.getComponentType();
                Object array = Array.newInstance(arrayType, num);
                for(int i=0;i<num;i++) {
                    JsonNode childNode = arrayNode.get(i);
                    Object arrValue = read(childNode, arrayType, parameterizedTypes);
                    Array.set(array, i, arrValue);
                }

                return array;
            }

            throw new ObjectMapException("Content [" + node + "] is not an array!");
        }
        else if(List.class.isAssignableFrom(cls)){
            if(node instanceof JsonArrayNode arrayNode){
                int num = arrayNode.size();

                List<Object> list = new ArrayList<>();
                for(int i=0;i<num;i++) {
                    JsonNode childNode = arrayNode.get(i);
                    AnalyzedParameterizedType analyzedChildType = parameterizedTypes.get("E");
                    Class<?> childType = analyzedChildType == null ? Object.class : analyzedChildType.getType();
                    Object arrValue = _read(childNode, childType);
                    list.add(arrValue);
                }

                return list;
            }

            throw new ObjectMapException("Content [" + node + "] is not an array!");
        }
        else if(Set.class.isAssignableFrom(cls)){
            if(node instanceof JsonArrayNode arrayNode){
                int num = arrayNode.size();

                Set<Object> set = new HashSet<>();
                for(int i=0;i<num;i++) {
                    JsonNode childNode = arrayNode.get(i);
                    AnalyzedParameterizedType analyzedChildType = parameterizedTypes.get("E");
                    Class<?> childType = analyzedChildType == null ? Object.class : analyzedChildType.getType();
                    Object arrValue = _read(childNode, childType);
                    set.add(arrValue);
                }

                return set;
            }

            throw new ObjectMapException("Content [" + node + "] is not an array!");
        }
        else if(Map.class.isAssignableFrom(cls)){
            if(node instanceof JsonObjectNode objectNode){
                if(parameterizedTypes.size() < 2) throw new ObjectMapException("Expected at least 2 parameterized-type-infos.");


                AnalyzedParameterizedType analyzedKeyType = parameterizedTypes.get("K");
                if(analyzedKeyType == null || analyzedKeyType.getType() != String.class) throw new ObjectMapException("Json objects can only map from string to something! Got: " + analyzedKeyType);

                AnalyzedParameterizedType analyzedValueType = parameterizedTypes.get("V");
                Class<?> valueType = analyzedValueType == null ? Object.class : analyzedValueType.getType();

                Map<String, Object> map = new HashMap<>();
                Map<String, JsonNode> objectNodeMap = objectNode.objectChildren();
                for(Map.Entry<String, JsonNode> entry : objectNodeMap.entrySet()) {
                    JsonNode childNode = entry.getValue();
                    Object arrValue = _read(childNode, valueType);
                    map.put(entry.getKey(), arrValue);
                }

                return map;
            }

            throw new ObjectMapException("Content [" + node + "] is not an map!");
        }
        else if(node instanceof JsonObjectNode objectNode) {
            AnalyzedPropertyClass analyzedClass;
            try {
                analyzedClass = analyzer.analyzeProperty(cls, parameterizedTypes);
            } catch (ClassAnalyzeException e) {
                throw new ObjectMapException("Failed to analyze class [" + cls.getName() + "]!", e);
            }

            Object instance = createInstanceFromEmptyConstructor(cls);

            Map<String, PropertyEntry> fieldEntries = analyzedClass.getFieldEntryMap();
            for (Map.Entry<String, PropertyEntry> entry : fieldEntries.entrySet()) {
                String fieldName = entry.getKey();
                JsonNode fieldNode = getNodeForAnalyzedFieldName(fieldName, objectNode);
                if(fieldNode != null){
                    PropertyEntry fieldEntry = entry.getValue();
                    Object fieldValue = readObject(fieldNode, fieldEntry.getParameterizedType());
                    IClassPropertySetter setter = fieldEntry.getSetter();

                    if(setter != null){
                        try {
                            setter.set(instance, fieldValue);
                        }
                        catch (InvocationTargetException e){
                            throw new ObjectMapException("Failed to set value for field: [" + fieldName + "]!", e);
                        }
                    }
                }
            }

            return instance;
        }
        else {
            throw new ObjectMapException("Invalid State: could not map type: [" + cls + "] for json node: " + node);
        }
    }

    private Object readObject(JsonNode node, AnalyzedParameterizedType analyzedType) throws ObjectMapException {
        Class<?> cls = analyzedType.getType();

        if(specializedLookup != null) {
            IObjectMapper<JsonNode, ?> reader = specializedLookup.get(JsonNode.class, cls);
            if (reader != null) return reader._read(node, cls);
        }

        if(ValueUtils.isPrimitiveOrBoxed(cls)){
            if(node instanceof JsonValueNode valueNode) {
                return ValueUtils.parsePrimitive(valueNode.get(), cls);
            }

            throw new ObjectMapException("Content [" + node + "] cannot be parsed to primitive value [" + cls.getName() + "]!");
        }
        else if(cls == String.class){
            if(node instanceof JsonValueNode valueNode) {
                return valueNode.value();
            }

            throw new ObjectMapException("Content [" + node + "] cannot be parsed to String!");
        }
        else if(cls.isArray()){
            if(node instanceof JsonArrayNode arrayNode){
                int num = arrayNode.size();
                Class<?> arrayType = cls.getComponentType();
                Object array = Array.newInstance(arrayType, num);
                for(int i=0;i<num;i++) {
                    JsonNode childNode = arrayNode.get(i);
                    Object arrValue = read(childNode, arrayType, analyzedType.getParameterizedTypeMap());
                    Array.set(array, i, arrValue);
                }

                return array;
            }

            throw new ObjectMapException("Content [" + node + "] is not an array!");
        }
        else if(List.class.isAssignableFrom(cls)){
            if(node instanceof JsonArrayNode arrayNode){
                int num = arrayNode.size();
                AnalyzedParameterizedType contentType = analyzedType.getParameterizedType("E");

                List<Object> list = new ArrayList<>();
                for(int i=0;i<num;i++) {
                    JsonNode childNode = arrayNode.get(i);
                    Object arrValue = readObject(childNode, contentType);
                    list.add(arrValue);
                }

                return list;
            }

            throw new ObjectMapException("Content [" + node + "] is not an array!");
        }
        else if(Set.class.isAssignableFrom(cls)){
            if(node instanceof JsonArrayNode arrayNode){
                int num = arrayNode.size();
                AnalyzedParameterizedType contentType = analyzedType.getParameterizedType("E");

                Set<Object> set = new HashSet<>();
                for(int i=0;i<num;i++) {
                    JsonNode childNode = arrayNode.get(i);
                    Object arrValue = readObject(childNode, contentType);
                    set.add(arrValue);
                }

                return set;
            }

            throw new ObjectMapException("Content [" + node + "] is not an array!");
        }
        else if(Map.class.isAssignableFrom(cls)){
            if(node instanceof JsonObjectNode objectNode){
                AnalyzedParameterizedType keyType = analyzedType.getParameterizedType("K");
                AnalyzedParameterizedType valueType = analyzedType.getParameterizedType("V");

                if(keyType.getType() != String.class) throw new ObjectMapException("Json objects can only map from string to something! Got: " + keyType);

                Map<String, Object> map = new HashMap<>();
                Map<String, JsonNode> objectNodeMap = objectNode.objectChildren();
                for(Map.Entry<String, JsonNode> entry : objectNodeMap.entrySet()) {
                    JsonNode childNode = entry.getValue();
                    Object arrValue = readObject(childNode, valueType);
                    map.put(entry.getKey(), arrValue);
                }

                return map;
            }

            throw new ObjectMapException("Content [" + node + "] is not an map!");
        }
        else if(node instanceof JsonObjectNode objectNode) {
            AnalyzedPropertyClass analyzedClass;
            try {
                analyzedClass = analyzer.analyzeProperty(cls, analyzedType.getParameterizedTypeMap());
            } catch (ClassAnalyzeException e) {
                throw new ObjectMapException("Failed to analyze class [" + cls.getName() + "]!", e);
            }

            Object instance = createInstanceFromEmptyConstructor(cls);

            Map<String, PropertyEntry> fieldEntries = analyzedClass.getFieldEntryMap();
            for (Map.Entry<String, PropertyEntry> entry : fieldEntries.entrySet()) {
                String fieldName = entry.getKey();
                JsonNode fieldNode = getNodeForAnalyzedFieldName(fieldName, objectNode);
                if(fieldNode != null){
                    PropertyEntry fieldEntry = entry.getValue();
                    Object fieldValue = readObject(fieldNode, fieldEntry.getParameterizedType());
                    IClassPropertySetter setter = fieldEntry.getSetter();

                    if(setter != null){
                        try {
                            setter.set(instance, fieldValue);
                        }
                        catch (InvocationTargetException e){
                            throw new ObjectMapException("Failed to set value for field: [" + fieldName + "]!", e);
                        }
                    }
                }
            }

            return instance;
        }
        else {
            throw new ObjectMapException("Invalid State: could not map type: [" + cls + "] for json node: " + node);
        }
    }

    private JsonNode getNodeForAnalyzedFieldName(String name, JsonObjectNode objectNode){
        //TODO better comparing so a little bit of room exists to play around
        return objectNode.get(name);
    }

    private Object createInstanceFromEmptyConstructor(Class<?> cls) throws ObjectMapException {
        Constructor<?> con = null;
        Constructor<?>[] constructors = cls.getDeclaredConstructors();
        for(Constructor<?> constructor : constructors){
            if(constructor.getParameterCount() == 0){
                con = constructor;
            }
        }

        if(con == null) throw new ObjectMapException("No empty constructor defined for class [" + cls.getName() + "]!");
        boolean needsAccess = !(Modifier.isPublic(cls.getModifiers()) && Modifier.isPublic(con.getModifiers()));

        try{
            if(needsAccess) con.setAccessible(true);
            return con.newInstance();
        }
        catch (IllegalAccessException e){
            throw new IllegalStateException("INVALID STATE: illegal access to constructor: [" + con + "] but it was set accessible!", e);
        }
        catch (InstantiationException e){
            throw new ObjectMapException("Class [" + cls.getName() + "] cannot be instantiated as it is abstract!", e);
        }
        catch (InvocationTargetException e){
            throw new ObjectMapException("Instantiating class [" + cls.getName() + "] threw an exception in the empty constructor!", e);
        }
        finally {
            if(needsAccess) con.setAccessible(false);
        }
    }

}
