package com.programm.projects.plugz.db.abstractbase.repo;

public interface IQueryExecutor {

    Object execute(String query) throws QueryExecuteException;

}
