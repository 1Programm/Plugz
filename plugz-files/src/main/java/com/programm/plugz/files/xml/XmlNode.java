package com.programm.plugz.files.xml;

import com.programm.plugz.files.ResourceNode;

import java.util.List;
import java.util.Map;

public interface XmlNode extends ResourceNode {

    String attrib(String name);

    Map<String, String> attribs();

    List<XmlNode> children();

}
