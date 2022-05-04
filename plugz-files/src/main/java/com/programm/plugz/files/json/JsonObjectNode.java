package com.programm.plugz.files.json;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class JsonObjectNode implements JsonNode {

    private final Map<String, JsonNode> children;

    @Override
    public String value() {
        return null;
    }

    public Map<String, JsonNode> objectChildren(){
        return children;
    }

    public JsonNode get(String name){
        return children.get(name);
    }

    @Override
    public List<JsonNode> children() {
        return new ArrayList<>(children.values());
    }

    @Override
    public String toString() {
        return JsonBuilder.toString(this);
    }
}
