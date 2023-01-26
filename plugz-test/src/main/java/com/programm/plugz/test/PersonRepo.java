package com.programm.plugz.test;

import com.programm.plugz.persist.Repo;

import java.util.List;

@Repo(Person.class)
public interface PersonRepo {

    List<Person> findAll();

    void save(Person person);

    void delete(Person person);

}
