package com.programm.plugz.webserv.api.request;

import com.programm.plugz.webserv.ex.WebservException;

public class RequestException extends WebservException {

    public RequestException(String message) {
        super(message);
    }

    public RequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestException(Throwable cause) {
        super(cause);
    }
}
