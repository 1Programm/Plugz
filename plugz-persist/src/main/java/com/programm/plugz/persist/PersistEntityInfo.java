package com.programm.plugz.persist;

import com.programm.plugz.cls.analyzer.AnalyzedPropertyClass;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class PersistEntityInfo {

    public final Class<?> entityClass;
    public final AnalyzedPropertyClass analyzedEntity;
    public final String primaryKey;
    public final Map<String, PersistForeignKeyInfo> foreignKeyInfoMap;

}
