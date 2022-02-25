package com.programm.projects.plugz.db.abstractbase.repo;

import com.programm.projects.plugz.magic.api.db.DataBaseException;

public class QueryExecuteException extends DataBaseException {

    public QueryExecuteException(String message) {
        super(message);
    }

    public QueryExecuteException(String message, Throwable cause) {
        super(message, cause);
    }
}
