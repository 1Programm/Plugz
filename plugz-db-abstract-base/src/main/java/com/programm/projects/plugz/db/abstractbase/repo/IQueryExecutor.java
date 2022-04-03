package com.programm.projects.plugz.db.abstractbase.repo;

public interface IQueryExecutor {

    Object execute(String query, Object... args) throws QueryExecuteException;

}
