package com.programm.plugz.test;

import com.programm.plugz.persist.Repo;

@Repo(Person.class)
public interface PersonRepo {

    void save(Person person);

    Person findById(int id);

}
