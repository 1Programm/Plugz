package com.programm.plugz.test;

import com.programm.plugz.persist.Entity;
import com.programm.plugz.persist.Generated;
import com.programm.plugz.persist.ID;

@Entity
public class Person {

    @ID
    @Generated
    private int id;
    private String name;
    private int age;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
