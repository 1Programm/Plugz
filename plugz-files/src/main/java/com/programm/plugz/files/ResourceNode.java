package com.programm.plugz.files;

import java.util.List;

public interface ResourceNode {

    String value();

    List<? extends ResourceNode> children();

}
