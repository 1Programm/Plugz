package com.programm.plugz.files.json;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class JsonValueNode implements JsonNode {

    private final Object value;

    @Override
    public String value() {
        return Objects.toString(value);
    }

    public Object get(){
        return value;
    }

    @Override
    public List<JsonNode> children() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return Objects.toString(value);
    }
}
