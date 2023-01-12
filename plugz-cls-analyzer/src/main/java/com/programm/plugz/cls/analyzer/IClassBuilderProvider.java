package com.programm.plugz.cls.analyzer;

public interface IClassBuilderProvider {

    IClassPropertyBuilder get(AnalyzedParameterizedType propertyClass);

}
