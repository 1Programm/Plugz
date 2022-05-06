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

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class PropertyObjectJsonNodeMapper implements IObjectMapper<Object, JsonNode> {

    private final ClassAnalyzer analyzer;
    private final ISpecializedObjectMapperLookup specializedLookup;
    private final boolean escapeInnerString;

    public PropertyObjectJsonNodeMapper(ISpecializedObjectMapperLookup specializedLookup) {
        this(new ClassAnalyzer(true, false), specializedLookup, false);
    }

    public PropertyObjectJsonNodeMapper(ClassAnalyzer analyzer, ISpecializedObjectMapperLookup specializedLookup, boolean escapeInnerString) {
        this.analyzer = analyzer;
        this.specializedLookup = specializedLookup;
        this.escapeInnerString = escapeInnerString;
    }

    @Override
    public JsonNode read(Object entity, Class<? extends JsonNode> cls) throws ObjectMapException {
        return read(entity, Collections.emptyMap());
    }

    public JsonNode read(Object entity) throws ObjectMapException {
        return read(entity, Collections.emptyMap());
    }

    public JsonNode read(Object entity, Map<String, AnalyzedParameterizedType> parameterizedTypes) throws ObjectMapException {
        if(entity == null) throw new ObjectMapException("Cannot map null elements!");

        Class<?> cls = entity.getClass();

        if(specializedLookup != null) {
            IObjectMapper<?, JsonNode> mapper = specializedLookup.get(cls, JsonNode.class);
            if (mapper != null) return mapper._read(entity, JsonNode.class);
        }

        if(cls == String.class){
            String _entity = entity.toString();
            if(escapeInnerString) _entity = escapeEscapeSequences(_entity);
            return new JsonValueNode(_entity);
        }
        else if(ValueUtils.isPrimitiveOrBoxed(cls)) {
            return new JsonValueNode(entity);
        }
        else if(cls.isArray()){
            Object[] arrEntity = (Object[]) entity;
            List<JsonNode> arrList = new ArrayList<>();

            for(Object e : arrEntity){
                if(e == null) continue;
                arrList.add(read(e));
            }

            return new JsonArrayNode(arrList);
        }
        else if(List.class.isAssignableFrom(cls)){
            List<?> arrEntity = (List<?>) entity;
            List<JsonNode> arrList = new ArrayList<>();

            for(Object e : arrEntity){
                if(e == null) continue;
                arrList.add(read(e));
            }

            return new JsonArrayNode(arrList);
        }
        else if(Set.class.isAssignableFrom(cls)){
            Set<?> arrEntity = (Set<?>) entity;
            List<JsonNode> arrList = new ArrayList<>();

            for(Object e : arrEntity){
                if(e == null) continue;
                arrList.add(read(e));
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
                JsonNode valueNode = read(value);
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
                    JsonNode entryValueNode = read(entryValueInstance);
                    theObject.put(entry.getKey(), entryValueNode);
                }
                catch (InvocationTargetException e){
                    throw new ObjectMapException("Failed to get property from getter.", e);
                }
            }

            return new JsonObjectNode(theObject);
        }
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
            else if(c == '\"'){
                sb.append(s, last, i);
                sb.append("\\\"");
                last = i + 1;
            }
        }

        if(last == 0) return s;

        if(last < sLen){
            sb.append(s, last, sLen);
        }

        return sb.toString();
    }

}
