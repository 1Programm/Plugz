package com.programm.projects.plugz.magic.api;

public class MagicResourceException extends Exception {

    public MagicResourceException(String message) {
        super(message);
    }

    public MagicResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}