package com.programm.plugz.files.yaml;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
class YamlMapNode implements YamlNode {

    private final String name;
    private final List<YamlNode> children;

    private final Map<String, YamlNode> cachedChildren = new HashMap<>();

    @Override
    public String name() {
        return name;
    }

    @Override
    public String value() {
        return null;
    }

    @Override
    public YamlNode getChild(Object o) {
        String name = o.toString();

        YamlNode cachedChild = cachedChildren.get(name);
        if(cachedChild != null){
            return cachedChild;
        }

        for(YamlNode child : children){
            String cName = child.name();
            if(cName.equals(name)){
                cachedChildren.put(cName, child);
                return child;
            }
        }

        return null;
    }

    @Override
    public List<YamlNode> children() {
        return children;
    }

    @Override
    public String toString() {
        return YamlBuilder.toString(this);
    }
}
