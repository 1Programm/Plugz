package com.programm.projects.plugz.db.abstractbase.repo;

import java.lang.reflect.Method;
import java.util.Map;

public class RepoEntry {

    private final Class<?> repoCls;
    private final Class<?> dataCls;
    private final Class<?> idCls;
    private final Map<Method, String> queries;

    public RepoEntry(Class<?> repoCls, Class<?> dataCls, Class<?> idCls, Map<Method, String> queries) {
        this.repoCls = repoCls;
        this.dataCls = dataCls;
        this.idCls = idCls;
        this.queries = queries;
    }

    public Class<?> getRepoCls() {
        return repoCls;
    }

    public Class<?> getDataCls() {
        return dataCls;
    }

    public Class<?> getIdCls() {
        return idCls;
    }

    public Map<Method, String> getQueries() {
        return queries;
    }
}
