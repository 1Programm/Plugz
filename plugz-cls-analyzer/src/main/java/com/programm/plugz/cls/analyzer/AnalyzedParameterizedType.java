package com.programm.plugz.cls.analyzer;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class AnalyzedParameterizedType {

    private final Class<?> type;
    private final AnalyzedParameterizedType parent;
    private final Map<String, AnalyzedParameterizedType> parameterizedTypeMap;

    public Class<?> getType() {
        return type;
    }

    public AnalyzedParameterizedType getParent(){
        return parent;
    }

    public Map<String, AnalyzedParameterizedType> getParameterizedTypeMap() {
        return parameterizedTypeMap;
    }

    public AnalyzedParameterizedType getParameterizedType(String name){
        return parameterizedTypeMap.get(name);
    }

    @Override
    public String toString() {
        if(parameterizedTypeMap.isEmpty()){
            return type.toString();
        }

        StringBuilder sb = new StringBuilder();
        for(String name : parameterizedTypeMap.keySet()){
            if(sb.length() == 0) sb.append("<");
            else sb.append(",");
            AnalyzedParameterizedType type = parameterizedTypeMap.get(name);
            sb.append(type);
        }

        sb.append(">");

        return type + sb.toString();
    }
}
