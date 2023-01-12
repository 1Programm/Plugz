package com.programm.plugz.persist.query;

import com.programm.plugz.persist.ex.PersistQueryExecuteException;

public interface IQuery {

    Object execute(Object... args) throws PersistQueryExecuteException;

}
