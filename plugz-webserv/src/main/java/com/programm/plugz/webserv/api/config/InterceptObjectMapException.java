package com.programm.plugz.webserv.api.config;

import com.programm.plugz.webserv.ex.WebservException;

public class InterceptObjectMapException extends WebservException {

    public InterceptObjectMapException(String message) {
        super(message);
    }

    public InterceptObjectMapException(String message, Throwable cause) {
        super(message, cause);
    }

    public InterceptObjectMapException(Throwable cause) {
        super(cause);
    }
}
