package com.programm.projects.plugz.magic.api.resources;

import com.programm.projects.plugz.magic.api.ISubsystem;

import java.net.URL;

public interface IResourcesManager extends ISubsystem {

    void removeUrl(URL url);

    Object buildMergedResourceObject(Class<?> cls) throws MagicResourceException;

    Object buildResourceObject(Class<?> cls) throws MagicResourceException;

}
