package com.programm.projects.plugz.db.abstractbase.repo;

import com.programm.projects.plugz.db.abstractbase.entity.EntityEntry;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

@RequiredArgsConstructor
public class RepoProxyHandler implements InvocationHandler {

    private final RepoEntry repoEntry;
    private final IQueryExecutor queryExecutor;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String mName = method.getName();

        switch (mName){
            case "getClass":
            case "equals":
            case "toString":
            case "hashCode":
            case "notify":
            case "notifyAll":
            case "wait":
                return method.invoke(proxy, args);
        }

        Map<Method, String> queries = repoEntry.getQueries();
        String query = queries.get(method);

        if(query != null) {
            return queryExecutor.execute(query);
        }

        throw new IllegalStateException("Method [" + method + "] is not checked by the system!");
    }
}
