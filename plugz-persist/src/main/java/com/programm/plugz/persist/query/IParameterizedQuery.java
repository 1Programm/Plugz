package com.programm.plugz.persist.query;

import com.programm.plugz.persist.ex.PersistQueryExecuteException;

public interface IParameterizedQuery <T> extends IQuery {

    @Override
    T execute(Object... arg) throws PersistQueryExecuteException;

}
