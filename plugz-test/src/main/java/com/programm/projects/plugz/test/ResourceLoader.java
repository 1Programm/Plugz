package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.api.IResourceLoader;

import java.util.List;

public class ResourceLoader implements IResourceLoader {

    @Override
    public Object[] loadFields(String resourceName, boolean staticResource, int notFound, List<Entry> entries) {
        return new Object[] { "Hello World!" };
    }
}
