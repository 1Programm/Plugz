package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.api.db.Builder;
import com.programm.projects.plugz.magic.api.db.Entity;

@Entity
public class TestUser {

    private final int id;
    private String name;

    @Builder("id")
    public TestUser(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
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
