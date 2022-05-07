package com.programm.plugz.webserv.api.config;

import com.programm.plugz.webserv.Cookie;
import com.programm.plugz.webserv.api.request.IRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
public class InterceptedRequest {

    public enum Type {
        OK,
        CONTINUE,
        FORWARD,
        REDIRECT,
        ERROR
    }

    private final String origin;
    private final IRequest request;
    private final Map<String, Cookie> newCookies;
    private final String contentType;
    private final Object responseBody;
    private final Type type;
    private Integer errStatus;
    private String errMsg;

}
