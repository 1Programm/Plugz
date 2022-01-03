package com.programm.projects.plugz.magic.api;

import java.net.URL;

public interface IResourcesManager {

    void startup() throws MagicResourceException;

    void shutdown() throws MagicResourceException;

    void removeUrl(URL url);

    Object buildMergedResourceObject(Class<?> cls) throws MagicResourceException;

    Object buildResourceObject(Class<?> cls) throws MagicResourceException;

}
