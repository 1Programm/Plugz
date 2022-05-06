package com.programm.plugz.object.mapper;

public interface IObjectMapper<D, E> {

    E read(D data, Class<? extends E> cls) throws ObjectMapException;

    @SuppressWarnings("unchecked")
    default E _read(Object data, Class<?> cls) throws ObjectMapException {
        return read((D) data, (Class<? extends E>) cls);
    }

}
