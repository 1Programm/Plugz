package com.programm.plugz.webserv.api.request;

import com.programm.plugz.webserv.Cookie;

public interface IUnprocessedRequest extends IRequest {

    String origin();

    IUnprocessedRequest param(String name, String value);

    IUnprocessedRequest header(String name, String value);

    @Override
    IUnprocessedRequest setCookie(Cookie cookie);

    IUnprocessedRequest setRequestBody(Object requestBody);

}
