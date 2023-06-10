package com.programm.plugz.persist.imbedded;

import com.programm.plugz.cls.analyzer.AnalyzedPropertyClass;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
class MethodQueryInfo {
    public final StatementType type;
    public final String query;
    public final AnalyzedPropertyClass returnType;
    public final List<Class<?>> parameterTypes;
    public final Object[] statementArguments;
    public final GeneratedKeysCallback generatedKeysCallback;
}
