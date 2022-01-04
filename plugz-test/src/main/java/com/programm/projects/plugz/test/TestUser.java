package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.api.db.Entity;

@Entity
public class TestUser {

    private final int id;

    public TestUser(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "TestUser{" +
                "id=" + id +
                '}';
    }
}
