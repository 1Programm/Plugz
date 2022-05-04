package com.programm.plugz.object.mapper;

public interface IObjectReader<D> {

    Object read(D data, Class<?> cls) throws ObjectMapException;

}
