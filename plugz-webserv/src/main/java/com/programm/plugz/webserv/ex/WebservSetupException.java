package com.programm.plugz.webserv.ex;

public class WebservSetupException extends WebservException {

    public WebservSetupException(String message) {
        super(message);
    }

    public WebservSetupException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebservSetupException(Throwable cause) {
        super(cause);
    }
}
