package com.programm.projects.plugz.magic;

public class MagicRuntimeException extends RuntimeException {

    public MagicRuntimeException() {
    }

    public MagicRuntimeException(String message) {
        super(message);
    }

    public MagicRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public MagicRuntimeException(Throwable cause) {
        super(cause);
    }
}
