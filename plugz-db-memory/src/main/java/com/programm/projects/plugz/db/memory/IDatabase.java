package com.programm.projects.plugz.db.memory;

import com.programm.projects.plugz.magic.api.db.DataBaseException;

import java.util.List;
import java.util.Map;

public interface IDatabase <ID, Data> {

    ID getAdvanceId();

    int size();

    Data save(ID id, Data data) throws DataBaseException;

    void remove(Data data) throws DataBaseException;

    List<Data> getAll();

    List<Data> query(List<List<String>> query, Map<String, Object> queryArgs);

}
