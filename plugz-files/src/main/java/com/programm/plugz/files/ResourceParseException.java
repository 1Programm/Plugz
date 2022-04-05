package com.programm.plugz.files;

public class ResourceParseException extends Exception {

    public ResourceParseException(String message) {
        super(message);
    }

    public ResourceParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceParseException(Throwable cause) {
        super(cause);
    }
}
