package com.programm.projects.plugz.magic.api.db;

public interface ICrudRepo <ID, Data> extends IRepo<ID, Data> {

    Data create(Object... arguments);

    Data findById(ID id);

    int size();

}
