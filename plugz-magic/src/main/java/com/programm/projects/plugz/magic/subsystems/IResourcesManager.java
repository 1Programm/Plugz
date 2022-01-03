package com.programm.projects.plugz.magic.subsystems;

import com.programm.projects.plugz.magic.resource.MagicResourceException;

public interface IResourcesManager {

    void startup() throws MagicResourceException;

    void shutdown() throws MagicResourceException;

    Object buildMergedResourceObject(Class<?> cls) throws MagicResourceException;

    Object buildResourceObject(Class<?> cls) throws MagicResourceException;

}
