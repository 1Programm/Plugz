package com.programm.plugz.files.yaml;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
class YamlTextNode implements YamlNode {

    private final String name;
    private final String value;

    @Override
    public String name() {
        return name;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public YamlNode getChild(Object o) {
        return null;
    }

    @Override
    public List<YamlNode> children() {
        return Collections.emptyList();
    }
}
