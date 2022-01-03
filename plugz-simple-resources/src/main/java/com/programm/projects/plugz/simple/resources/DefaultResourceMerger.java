package com.programm.projects.plugz.simple.resources;

import com.programm.projects.plugz.magic.api.IResourceMerger;

public class DefaultResourceMerger implements IResourceMerger {

    @Override
    public Object mergeValues(String name, Object originalValue, Object newValue) {
        return newValue == null ? originalValue : newValue; //Simply override original value
    }
}
