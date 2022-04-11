package com.programm.plugz.webserv;

import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.ISubsystem;
import com.programm.plugz.api.ISubsystemSetupHelper;
import com.programm.plugz.api.MagicException;
import com.programm.plugz.api.auto.Get;
import com.programm.projects.ioutils.log.api.out.ILogger;

public class WebservSubsystem implements ISubsystem {

    @Get private ILogger log;

    @Override
    public void registerSetup(ISubsystemSetupHelper setupHelper, AnnotationChecker annocheck) throws MagicException {
        
    }

    @Override
    public void startup() throws MagicException {

    }

    @Override
    public void shutdown() throws MagicException {

    }
}
