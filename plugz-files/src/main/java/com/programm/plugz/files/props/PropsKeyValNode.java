package com.programm.plugz.files.props;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
class PropsKeyValNode implements PropsNode {

    private final String name;
    private final String value;

    @Override
    public String name() {
        return name;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public List<PropsNode> children() {
        return Collections.emptyList();
    }
}
