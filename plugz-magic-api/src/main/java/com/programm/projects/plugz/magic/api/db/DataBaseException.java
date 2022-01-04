package com.programm.projects.plugz.magic.api.db;

import com.programm.projects.plugz.magic.api.MagicException;

public class DataBaseException extends MagicException {

    public DataBaseException(String message) {
        super(message);
    }

    public DataBaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
