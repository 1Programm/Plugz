package com.programm.plugz.files;

import java.util.List;

public interface NamedResourceNode extends ResourceNode {

    String name();

    List<? extends NamedResourceNode> children();

}
