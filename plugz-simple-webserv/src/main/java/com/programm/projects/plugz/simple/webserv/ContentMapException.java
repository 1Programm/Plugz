package com.programm.projects.plugz.simple.webserv;

import com.programm.projects.plugz.magic.api.MagicException;

public class ContentMapException extends MagicException {

    public ContentMapException(String message) {
        super(message);
    }

    public ContentMapException(String message, Throwable cause) {
        super(message, cause);
    }
}
