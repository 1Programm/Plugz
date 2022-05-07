package com.programm.plugz.webserv.api.config;

import com.programm.plugz.webserv.api.IRequestValidator;
import com.programm.plugz.webserv.api.request.IExecutableRequest;
import com.programm.plugz.webserv.api.request.IRequest;
import com.programm.plugz.webserv.api.request.IUnprocessedRequest;

import java.util.function.Function;

public interface IRestPathConfig {

    IRestPathConfig with(IRequestValidator validator);

    IRestPathConfig onUnauthorizedAccess(IRequestInterceptor onUnauthorizedAccess);

    default IRestPathConfig onUnauthorizedAccessReplyError(int status, String message) {
        return onUnauthorizedAccess((handler, req) -> req.doError(status, message));
    }

    default IRestPathConfig onUnauthorizedAccessReplyError() {
        return onUnauthorizedAccessReplyError(401, "Unauthorized");
    }

    default IRestPathConfig onUnauthorizedAccessRedirect(Function<IRequestHandler, IUnprocessedRequest> requestBuilder) {
        return onUnauthorizedAccess((handler, req) -> req.doRedirect(requestBuilder.apply(handler)));
    }

    default IRestPathConfig onUnauthorizedAccessRedirect(String path) {
        return onUnauthorizedAccessRedirect(handler -> handler.buildRequest(path));
    }

    void onSuccess(IRequestInterceptor onSuccess);

    default void onSuccess(Function<IExecutableRequest, IInterceptedRequestAction> onSuccess){
        onSuccess((handler, req) -> onSuccess.apply(req));
    }

    default void onSuccessOk(){
        onSuccess((Function<IExecutableRequest, IInterceptedRequestAction>) IExecutableRequest::doOk);
    }

    default void onSuccessContinue(){
        onSuccess((Function<IExecutableRequest, IInterceptedRequestAction>) IExecutableRequest::doContinue);
    }

}
