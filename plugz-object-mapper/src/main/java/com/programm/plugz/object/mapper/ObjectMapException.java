package com.programm.plugz.object.mapper;

public class ObjectMapException extends Exception {

    public ObjectMapException() {
    }

    public ObjectMapException(String message) {
        super(message);
    }

    public ObjectMapException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectMapException(Throwable cause) {
        super(cause);
    }
}
