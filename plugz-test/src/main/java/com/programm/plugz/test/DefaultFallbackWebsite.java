package com.programm.plugz.test;

import com.programm.plugz.webserv.api.config.IInterceptedRequestAction;
import com.programm.plugz.webserv.api.config.IRequestHandler;
import com.programm.plugz.webserv.api.config.IRequestInterceptor;
import com.programm.plugz.webserv.api.request.IExecutableRequest;

public class DefaultFallbackWebsite implements IRequestInterceptor {

    @Override
    public IInterceptedRequestAction onRequest(IRequestHandler handler, IExecutableRequest request) {
        return request.doOk("text/html", "<h1 style=\"text-align: center;\">404 Page [" + request.query() + "] could not be found!</h1>");
    }
}
