package com.programm.plugz.webserv.api.request;

import com.programm.plugz.webserv.api.config.IInterceptedRequestAction;

public interface IExecutableRequest extends IRequest {

    IInterceptedRequestAction doContinue();

    IInterceptedRequestAction doContinue(IUnprocessedRequest newRequest);

    IInterceptedRequestAction doCancel();

    IInterceptedRequestAction doRedirect(IUnprocessedRequest newRequest);

    IInterceptedRequestAction doError(int status, String message);

}
