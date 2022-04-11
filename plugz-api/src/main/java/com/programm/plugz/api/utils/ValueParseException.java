package com.programm.plugz.api.utils;

public class ValueParseException extends RuntimeException {

    public ValueParseException(String message) {
        super(message);
    }

    public ValueParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValueParseException(Throwable cause) {
        super(cause);
    }
}
