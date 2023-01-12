package com.programm.plugz.persist.imbedded;

import com.programm.plugz.cls.analyzer.AnalyzedPropertyClass;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class MethodQueryInfo {
    public final String query;
    public final AnalyzedPropertyClass returnType;
    public final Class<?>[] parameterTypes;
}
