package com.programm.projects.plugz.magic.api.db;

import java.util.List;

public interface ICrudRepo <ID, Data> extends IRepo<ID, Data> {

    Data save(Data data);

    void remove(Data data);

    List<Data> findAll();

    Data findById(ID id);

    int size();

}
