package com.programm.plugz.api;

public class MagicSetupException extends MagicException {

    public MagicSetupException(String message) {
        super(message);
    }

    public MagicSetupException(String message, Throwable cause) {
        super(message, cause);
    }

    public MagicSetupException(Throwable cause) {
        super(cause);
    }
}
