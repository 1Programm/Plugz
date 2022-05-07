package com.programm.plugz.webserv.api.request;

public interface IUnprocessedRequest extends IRequest {

    String origin();

    default IUnprocessedRequest param(String name, String value){
        params().put(name, value);
        return this;
    }

    default IUnprocessedRequest header(String name, String value){
        headers().put(name, value);
        return this;
    }

    IUnprocessedRequest setRequestBody(Object requestBody);

}
