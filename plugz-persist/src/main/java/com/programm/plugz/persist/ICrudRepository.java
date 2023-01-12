package com.programm.plugz.persist;

import java.util.Collection;

public interface ICrudRepository<ID, Data> {

    Collection<Data> findAll();

    Data findById(ID id);

    Data update(Data data);

    Data delete(ID id);

}
