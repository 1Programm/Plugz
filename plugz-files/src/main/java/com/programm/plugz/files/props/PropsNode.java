package com.programm.plugz.files.props;

import com.programm.plugz.files.ResourceNode;

import java.util.List;

public interface PropsNode extends ResourceNode {

    List<PropsNode> children();

}
