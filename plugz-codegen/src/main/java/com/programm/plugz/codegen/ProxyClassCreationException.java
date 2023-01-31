package com.programm.plugz.codegen;

public class ProxyClassCreationException extends Exception {

    public ProxyClassCreationException(String message) {
        super(message);
    }

    public ProxyClassCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyClassCreationException(Throwable cause) {
        super(cause);
    }

    public ProxyClassCreationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
