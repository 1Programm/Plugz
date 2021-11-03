package com.programm.projects.plugz;

public class ConfigFailedException extends RuntimeException {

    public ConfigFailedException() {
    }

    public ConfigFailedException(String message) {
        super(message);
    }

    public ConfigFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigFailedException(Throwable cause) {
        super(cause);
    }
}
