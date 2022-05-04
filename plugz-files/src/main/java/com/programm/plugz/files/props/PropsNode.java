package com.programm.plugz.files.props;

import com.programm.plugz.files.NamedResourceNode;
import com.programm.plugz.files.ResourceNode;

import java.util.List;

public interface PropsNode extends NamedResourceNode {

    List<PropsNode> children();

}
