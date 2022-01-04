package com.programm.projects.plugz.magic.api.resources;

import java.util.List;

public interface IResourceLoader {

    interface Entry {
        String name();
        Class<?> type();
        IValueFallback fallback();
    }

    interface Result {
        int size();
        Object get(int i);
        void save(String[] names, Object[] values) throws MagicResourceException;
    }

    Result loadFields(String resourceName, int notFound, List<Entry> entries);

}
