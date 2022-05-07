package com.programm.plugz.webserv.api.config;

import com.programm.plugz.webserv.api.request.IExecutableRequest;

public interface IRequestInterceptor {

    IInterceptedRequestAction onRequest(IRequestHandler handler, IExecutableRequest request) throws InterceptPathException;

}
