package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.api.db.Entity;

@Entity
public class TestUser {

    /*

    1. id is generated when no id is set
    TestUser u1 = new TestUser();
    repo.save(u1); //sets the generated id or throws an exception if id is final

    2. id is not custom and replaces data
    TestUser u1 = new TestUser(1);
    repo.save(u1); //id is already set so it will set or replace existing data



     */

    private final Integer id;
    private String name;

    public TestUser(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "TestUser{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
