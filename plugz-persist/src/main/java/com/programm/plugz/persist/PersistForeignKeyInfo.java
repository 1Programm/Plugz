package com.programm.plugz.persist;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PersistForeignKeyInfo {

    public enum ConnectionType {
        ONE_TO_ONE,
        ONE_TO_MANY,
        MANY_TO_ONE,
        MANY_TO_MANY
    }

    public final ConnectionType connectionType;
    public final Class<?> foreignEntityType;
    public final String foreignKey;

}
