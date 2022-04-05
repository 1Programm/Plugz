package com.programm.plugz.files.yaml;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
class YamlArrayNode implements YamlNode {

    private final String name;
    private final List<YamlNode> children;

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
        if(o == null) return null;

        Class<?> cls = o.getClass();
        if(cls == Integer.class) {
            return children.get((Integer) o);
        }
        else if(cls == Integer.TYPE){
            return children.get((int) o);
        }

        return null;
    }

    @Override
    public List<YamlNode> children() {
        return children;
    }
}
