package com.programm.plugz.webserv.api.request;

import com.programm.plugz.webserv.Cookie;
import com.programm.plugz.webserv.api.config.IInterceptedRequestAction;

public interface IExecutableRequest extends IRequest {

    @Override
    IExecutableRequest setCookie(Cookie cookie);

    IInterceptedRequestAction doOk();

    IInterceptedRequestAction doOk(String contentType, Object responseBody);

    IInterceptedRequestAction doOk(Object responseBody);

    IInterceptedRequestAction doContinue();

    IInterceptedRequestAction doContinue(IUnprocessedRequest newRequest) throws InvalidRequestException;

    IInterceptedRequestAction doRedirect(IUnprocessedRequest newRequest) throws InvalidRequestException;

    IInterceptedRequestAction doError(int status, String message);

}
