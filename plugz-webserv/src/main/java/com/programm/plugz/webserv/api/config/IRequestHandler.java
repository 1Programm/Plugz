package com.programm.plugz.webserv.api.config;

import com.programm.plugz.webserv.RequestType;
import com.programm.plugz.webserv.api.request.IUnprocessedRequest;

public interface IRequestHandler {

    IUnprocessedRequest buildRequest(String origin, RequestType type, String path);

    default IUnprocessedRequest buildRequest(RequestType type, String path) {
        return buildRequest(null, type, path);
    }

    default IUnprocessedRequest buildRequest(String origin, String path) {
        return buildRequest(origin, RequestType.GET, path);
    }

    default IUnprocessedRequest buildRequest(String path) {
        return buildRequest(null, RequestType.GET, path);
    }

}
