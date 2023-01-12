package com.programm.plugz.persist;

import com.programm.plugz.cls.analyzer.AnalyzedPropertyClass;
import com.programm.plugz.cls.analyzer.ClassAnalyzer;
import com.programm.plugz.persist.ex.PersistQueryBuildException;
import com.programm.plugz.persist.ex.PersistShutdownException;
import com.programm.plugz.persist.ex.PersistStartupException;
import com.programm.plugz.persist.query.IParameterizedQuery;
import com.programm.plugz.persist.query.IQuery;

public interface IRepoHandler {

    void startup(ClassAnalyzer analyzer) throws PersistStartupException;

    void shutdown() throws PersistShutdownException;

    Object createRepoImplementation(Class<?> cls, AnalyzedPropertyClass analyzedEntityClass) throws PersistQueryBuildException;

    IQuery createQuery(String query) throws PersistQueryBuildException;

    <T> IParameterizedQuery<T> createQuery(String query, Class<T> cls) throws PersistQueryBuildException;

}
