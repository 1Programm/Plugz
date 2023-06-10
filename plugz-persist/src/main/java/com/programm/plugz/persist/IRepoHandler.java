package com.programm.plugz.persist;

import com.programm.plugz.cls.analyzer.ClassAnalyzer;
import com.programm.plugz.persist.ex.PersistQueryBuildException;
import com.programm.plugz.persist.ex.PersistShutdownException;
import com.programm.plugz.persist.ex.PersistStartupException;

import java.util.Map;

public interface IRepoHandler {

    void startup(ClassAnalyzer analyzer, Map<Class<?>, PersistEntityInfo> infoMap) throws PersistStartupException;

    void shutdown() throws PersistShutdownException;

    Object createRepoImplementation(Class<?> repoCls, PersistEntityInfo entityInfo, Map<Class<?>, PersistEntityInfo> infoMap) throws PersistQueryBuildException;

}
