package com.programm.plugz.files.xml;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
class XmlTextNode implements XmlNode {

    private final String content;

    @Override
    public String name() {
        return null;
    }

    @Override
    public String value() {
        return content;
    }

    @Override
    public String attrib(String name) {
        return null;
    }

    @Override
    public Map<String, String> attribs() {
        return Collections.emptyMap();
    }

    @Override
    public List<XmlNode> children() {
        return Collections.emptyList();
    }

}
