package com.programm.plugz.test;

import com.programm.plugz.persist.Repo;

@Repo(Tag.class)
public interface TagRepo {

    void save(Tag t);

}
