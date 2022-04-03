package com.programm.projects.plugz.magic.api.web;

import com.programm.projects.plugz.magic.api.MagicException;

public class MagicWebException extends MagicException {

    public MagicWebException(String message) {
        super(message);
    }

    public MagicWebException(String message, Throwable cause) {
        super(message, cause);
    }
}
