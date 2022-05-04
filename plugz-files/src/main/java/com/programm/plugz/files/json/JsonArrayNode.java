package com.programm.plugz.files.json;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class JsonArrayNode implements JsonNode {

    private final List<JsonNode> children;

    @Override
    public String value() {
        return null;
    }

    @Override
    public List<JsonNode> children() {
        return children;
    }

    public JsonNode get(int i) {
        return children.get(i);
    }

    public int size(){
        return children.size();
    }

    @Override
    public String toString() {
        return JsonBuilder.toString(this);
    }
}
