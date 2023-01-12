package com.programm.plugz.persist.aqua;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class QueryExecutionInfo {
    public final String tableName;
    public final String method;
    public final List<String> selections;
    public final QueryTerm conditions;

    @Override
    public String toString() {
        return tableName + "." + method + "(" + (selections == null ? "*" : "{" + String.join(", ", selections) + "}") + (conditions == null ? "" : ", " + conditions) + ")";
    }

}
