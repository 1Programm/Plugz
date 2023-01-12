package com.programm.plugz.test;

import com.programm.plugz.persist.CustomQuery;
import com.programm.plugz.persist.Repo;

import java.util.List;
import java.util.Map;

@Repo(Person.class)
public interface PersonRepo {

//    List<Person> getAll();
//
//    Person[] findAll();
//
//    List<Map<String, Object>> getAllNameAndAge();
//
//    List<PersonNameAgeDto> findAllNameAndAge();
//
//    @CustomQuery("SELECT * FROM person WHERE id = ?")
//    Map<String, Object> test(int id);

    Person getById(int id);

    void update(Person person);

}
