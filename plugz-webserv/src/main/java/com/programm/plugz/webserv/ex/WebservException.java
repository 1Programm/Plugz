package com.programm.plugz.webserv.ex;

import com.programm.plugz.api.MagicException;

public class WebservException extends MagicException {

    public WebservException(String message) {
        super(message);
    }

    public WebservException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebservException(Throwable cause) {
        super(cause);
    }
}
