package com.programm.plugz.files.yaml;

import com.programm.plugz.files.NamedResourceNode;
import com.programm.plugz.files.ResourceNode;

import java.util.List;

public interface YamlNode extends NamedResourceNode {

    YamlNode getChild(Object o);

    List<YamlNode> children();

}
