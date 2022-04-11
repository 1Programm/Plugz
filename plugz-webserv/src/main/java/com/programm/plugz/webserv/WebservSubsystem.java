package com.programm.plugz.webserv;

import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.ISubsystem;
import com.programm.plugz.api.ISubsystemSetupHelper;
import com.programm.plugz.api.MagicException;
import com.programm.plugz.api.PlugzConfig;
import com.programm.plugz.api.auto.Get;
import com.programm.projects.ioutils.log.api.out.ILogger;

public class WebservSubsystem implements ISubsystem {

    private static final int DEFAULT_PORT = 8080;

    @Get private ILogger log;
    @Get private PlugzConfig config;

    @Override
    public void registerSetup(ISubsystemSetupHelper setupHelper, AnnotationChecker annocheck) throws MagicException {

    }

    @Override
    public void startup() throws MagicException {
        int port = config.getIntOrDefault("webserv.port", DEFAULT_PORT);
        log.info("Starting Server on port [{}]...", port);
    }

    @Override
    public void shutdown() throws MagicException {

    }
}
