package com.programm.plugz.test;

import com.programm.plugz.persist.Entity;
import com.programm.plugz.persist.Generated;
import com.programm.plugz.persist.ID;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Person {

    @ID
    @Generated
    public long id;
    public String name;
    public int age;

}
