package com.programm.plugz.webserv.api.request;

import com.programm.plugz.webserv.RequestType;
import com.programm.plugz.webserv.api.config.InterceptObjectMapException;

import java.util.Map;

public interface IRequest {

    RequestType type();

    String query();


    Map<String, String> params();

    default String param(String name){
        return params().get(name);
    }


    Map<String, String> headers();

    default String header(String name) {
        return headers().get(name);
    }


    <T> T getRequestBody(Class<T> cls) throws InterceptObjectMapException;

    String buildFullQuery();

}
