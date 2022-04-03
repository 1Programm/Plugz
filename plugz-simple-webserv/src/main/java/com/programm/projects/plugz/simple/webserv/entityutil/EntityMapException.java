package com.programm.projects.plugz.simple.webserv.entityutil;

import com.programm.projects.plugz.magic.api.db.DataBaseException;

public class EntityMapException extends DataBaseException {

    public EntityMapException(String message) {
        super(message);
    }

    public EntityMapException(String message, Throwable cause) {
        super(message, cause);
    }
}
