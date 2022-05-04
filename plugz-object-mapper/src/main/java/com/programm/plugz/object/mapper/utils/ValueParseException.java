package com.programm.plugz.object.mapper.utils;

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
