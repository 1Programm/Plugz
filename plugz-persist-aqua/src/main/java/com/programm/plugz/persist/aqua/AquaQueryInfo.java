package com.programm.plugz.persist.aqua;

import com.programm.plugz.cls.analyzer.AnalyzedParameterizedType;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
class AquaQueryInfo {

    public final QueryExecutionInfo executionInfo;
    public final AnalyzedParameterizedType returnType;
    public final boolean array;
    public final boolean collection;
    public final List<AnalyzedParameterizedType> parameterTypes;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for(int i=0;i<parameterTypes.size();i++){
            if(i != 0) sb.append(", ");
            sb.append(parameterTypes.get(i));
        }
        sb.append(") -> ").append(executionInfo).append(" -> ").append(returnType);

        return sb.toString();
    }
}
