package com.programm.plugz.webserv.api.config;

import com.programm.plugz.webserv.api.request.IRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
public class InterceptedRequest {

    public enum Type {
        CONTINUE,
        FORWARD,
        REDIRECT,
        ERROR
    }

    private final String origin;
    private final IRequest request;
    private final Type type;
    private int errStatus;
    private String errMsg;

}
