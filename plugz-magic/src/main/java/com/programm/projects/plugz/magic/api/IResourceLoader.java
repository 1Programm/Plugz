package com.programm.projects.plugz.magic.api;

import java.util.List;

public interface IResourceLoader {

    interface Entry {
        String name();
        Class<?> type();
        IValueFallback fallback();
    }

    Object[] loadFields(String resourceName, boolean staticResource, int notFound, List<Entry> entries);

}
