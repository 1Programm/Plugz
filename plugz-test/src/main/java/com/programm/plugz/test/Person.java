package com.programm.plugz.test;

import com.programm.plugz.persist.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Entity
@Getter
@Setter
public class Person {

    @ID
    @Generated
    private int id;

    private String name;

    @ForeignKey("id")
//    private List<Tag> tag;
    private Tag tag;


    @CustomQuery("test")
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", tag=" + tag +
                '}';
    }
}
