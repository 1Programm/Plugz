package com.programm.plugz.persist.aqua;

import com.programm.plugz.cls.analyzer.AnalyzedPropertyClass;
import com.programm.plugz.persist.ex.PersistQueryBuildException;
import com.programm.plugz.persist.ex.PersistQueryExecuteException;

import java.util.List;

public interface IAquaRepositoryConnection {

    void createTable(String name, AnalyzedPropertyClass entityCls) throws PersistQueryBuildException;

    Object execute(String tableName, String method, List<String> selections, QueryTerm conditions) throws PersistQueryExecuteException;

}
