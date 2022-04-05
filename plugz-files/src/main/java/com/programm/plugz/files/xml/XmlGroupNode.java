package com.programm.plugz.files.xml;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
class XmlGroupNode implements XmlNode {

    private final String name;
    private final Map<String, String> attributes;
    final List<XmlNode> children;

    @Override
    public String name() {
        return name;
    }

    @Override
    public String value() {
        if(children.size() == 1 && children.get(0) instanceof XmlTextNode){
            return children.get(0).value();
        }

        return null;
    }

    @Override
    public String attrib(String name) {
        return attributes.get(name);
    }

    @Override
    public Map<String, String> attribs() {
        return attributes;
    }

    @Override
    public List<XmlNode> children() {
        if(children.size() == 1 && children.get(0) instanceof XmlTextNode){
            return Collections.emptyList();
        }

        return children;
    }

    @Override
    public String toString() {
        return XmlBuilder.toString(this);
    }
}
