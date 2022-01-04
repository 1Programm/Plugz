package com.programm.projects.plugz.magic.api;

public class MagicException extends Exception {

    public MagicException(String message) {
        super(message);
    }

    public MagicException(String message, Throwable cause) {
        super(message, cause);
    }

    public MagicException(Throwable cause) {
        super(cause);
    }
}
