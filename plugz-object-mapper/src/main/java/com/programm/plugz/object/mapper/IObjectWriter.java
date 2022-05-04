package com.programm.plugz.object.mapper;

public interface IObjectWriter<D, E> {

    D write(E entity) throws ObjectMapException;

    @SuppressWarnings("unchecked")
    default D _write(Object entity) throws ObjectMapException {
        return write((E) entity);
    }
}
