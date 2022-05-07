package com.programm.plugz.webserv.api.request;

import com.programm.plugz.webserv.RequestType;
import com.programm.plugz.webserv.Cookie;
import com.programm.plugz.webserv.api.config.InterceptObjectMapException;

import java.util.List;
import java.util.Map;

public interface IRequest {

    RequestType type();

    String query();


    Map<String, String> params();

    default String param(String name){
        return params().get(name);
    }


    Map<String, List<String>> headers();

    default List<String> header(String name) {
        return headers().get(name);
    }


    Map<String, Cookie> cookies();

    default Cookie cookie(String name) {
        return cookies().get(name);
    }

    IRequest setCookie(Cookie cookie);


    <T> T getRequestBody(Class<T> cls) throws InterceptObjectMapException;

    String buildFullQuery();

}
