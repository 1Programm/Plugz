package com.programm.plugz.webserv;

import com.programm.ioutils.log.api.ILogger;
import com.programm.plugz.annocheck.AnnotationChecker;
import com.programm.plugz.api.ISubsystem;
import com.programm.plugz.api.ISubsystemSetupHelper;
import com.programm.plugz.api.MagicException;
import com.programm.plugz.api.PlugzConfig;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.webserv.ex.WebservSetupException;

import java.net.ServerSocket;

public class WebservSubsystem implements ISubsystem {

    private static final String CONF_SERVER_PORT_NAME = "webserv.port";
    private static final int CONF_SERVER_PORT_DEFAULT = 8080;

    @Get private ILogger log;
    @Get private PlugzConfig config;

    @Override
    public void registerSetup(ISubsystemSetupHelper setupHelper, AnnotationChecker annocheck) throws MagicException {
        config.registerDefaultConfiguration(CONF_SERVER_PORT_NAME, CONF_SERVER_PORT_DEFAULT);
    }

    @Override
    public void startup() throws MagicException {
        int port = config.getIntOrError(CONF_SERVER_PORT_NAME, WebservSetupException::new);
        log.info("Starting Server on port [{}]...", port);
    }

    @Override
    public void shutdown() throws MagicException {

    }
}
