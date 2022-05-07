package com.programm.plugz.webserv.api.config;

import com.programm.plugz.webserv.ex.WebservException;

public class InterceptPathException extends WebservException {

    public InterceptPathException(String message) {
        super(message);
    }

    public InterceptPathException(String message, Throwable cause) {
        super(message, cause);
    }

    public InterceptPathException(Throwable cause) {
        super(cause);
    }
}
