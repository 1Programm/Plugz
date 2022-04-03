package com.programm.projects.plugz.magic.api.web;

import com.programm.projects.plugz.magic.api.ISubsystem;

public interface IWebServerManager extends ISubsystem {

    void registerRequestMethod(WebRequestMethodConfig methodConfig) throws MagicWebException;

}
