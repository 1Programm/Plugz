package com.programm.plugz.object.mapper;

public interface IConfigurableObjectReader<D> extends IObjectReader<D> {

    IConfigurableObjectReader<D> registerReader(Class<?> cls, IObjectReader<D> reader);

}
