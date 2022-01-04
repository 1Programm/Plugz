package com.programm.projects.plugz.magic.api.resources;

import com.programm.projects.plugz.magic.api.MagicException;

public class MagicResourceException extends MagicException {

    public MagicResourceException(String message) {
        super(message);
    }

    public MagicResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
