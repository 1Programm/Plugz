package com.programm.plugz.files.json;

import com.programm.plugz.files.ResourceNode;

import java.util.List;

public interface JsonNode extends ResourceNode {

    List<JsonNode> children();

}
