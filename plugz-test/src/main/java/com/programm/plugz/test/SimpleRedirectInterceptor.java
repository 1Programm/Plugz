package com.programm.plugz.test;

import com.programm.plugz.webserv.api.config.IInterceptedRequestAction;
import com.programm.plugz.webserv.api.config.IRequestHandler;
import com.programm.plugz.webserv.api.config.IRequestInterceptor;
import com.programm.plugz.webserv.api.request.IExecutableRequest;
import com.programm.plugz.webserv.api.request.RequestException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleRedirectInterceptor implements IRequestInterceptor {

    private final String origin;
    private final String path;

    @Override
    public IInterceptedRequestAction onRequest(IRequestHandler handler, IExecutableRequest request) throws RequestException {
        return request.doRedirect(handler.buildRequest(origin, request.type(), path));
    }
}
