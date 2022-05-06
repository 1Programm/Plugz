package com.programm.plugz.object.mapper;

public interface ISpecializedObjectMapperLookup {

    <D, E> IObjectMapper<D, E> get(Class<D> dataCls, Class<E> entityCls);

}
