package com.programm.projects.plugz.magic.api.resources;

public interface IResourceMerger {

    Object mergeValues(String name, Object originalValue, Object newValue);

}
