package com.programm.plugz.files.props;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
class PropsRootNode implements PropsNode {

    private final List<PropsNode> children;

    @Override
    public String name() {
        return null;
    }

    @Override
    public String value() {
        return null;
    }

    @Override
    public List<PropsNode> children() {
        return children;
    }
}
