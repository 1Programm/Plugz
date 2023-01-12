package com.programm.plugz.persist.ex;

public class PersistQueryExecuteException extends PersistRuntimeException {

    public PersistQueryExecuteException(String message) {
        super(message);
    }

    public PersistQueryExecuteException(String message, Throwable cause) {
        super(message, cause);
    }
}
