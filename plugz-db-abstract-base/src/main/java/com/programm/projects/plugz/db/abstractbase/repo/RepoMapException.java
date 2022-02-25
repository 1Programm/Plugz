package com.programm.projects.plugz.db.abstractbase.repo;

import com.programm.projects.plugz.magic.api.db.DataBaseException;

public class RepoMapException extends DataBaseException {

    public RepoMapException(String message) {
        super(message);
    }

    public RepoMapException(String message, Throwable cause) {
        super(message, cause);
    }

}
