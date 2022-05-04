package com.programm.plugz.object.mapper;

public interface IConfigurableObjectWriter <D, E> extends IObjectWriter<D, E> {

    IConfigurableObjectWriter<D, E> registerWriter(Class<?> cls, IObjectWriter<D, E> writer);

}
