package com.programm.plugz.persist.ex;

public class PersistRuntimeException extends RuntimeException {

    public PersistRuntimeException() {
    }

    public PersistRuntimeException(String message) {
        super(message);
    }

    public PersistRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistRuntimeException(Throwable cause) {
        super(cause);
    }

    public PersistRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
