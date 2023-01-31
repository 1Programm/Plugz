package com.programm.plugz.codegen;

public class ProxyClassRuntimeException extends RuntimeException {

    public ProxyClassRuntimeException(String message) {
        super(message);
    }

    public ProxyClassRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyClassRuntimeException(Throwable cause) {
        super(cause);
    }

    public ProxyClassRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
