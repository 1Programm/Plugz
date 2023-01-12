package com.programm.plugz.persist.aqua;

import com.programm.plugz.persist.ex.PersistQueryBuildException;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

@RequiredArgsConstructor
public class AquaRepoInvocationHandler implements InvocationHandler {

    private final Map<String, AquaQueryInfo> methodToQueryMap;
    private final AquaRepoHandler repoHandler;

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

        mName = method.toString();
        AquaQueryInfo query = methodToQueryMap.get(mName);
        if(query == null) throw new PersistQueryBuildException("Failed to parse method: [" + method + "] to query!");

        //TODO: Check param types + return type

        return repoHandler.executeQuery(query.executionInfo, query.returnType, query.array, query.collection, query.parameterTypes, args);
    }
}
