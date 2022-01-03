package com.programm.projects.plugz.magic.api;

public interface IResourcesManager {

    void startup() throws MagicResourceException;

    void shutdown() throws MagicResourceException;

    Object buildMergedResourceObject(Class<?> cls) throws MagicResourceException;

    Object buildResourceObject(Class<?> cls) throws MagicResourceException;

}
