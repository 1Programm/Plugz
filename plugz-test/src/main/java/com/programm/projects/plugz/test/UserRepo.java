package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.api.db.Repo;

@Repo
public interface UserRepo {

    TestUser getById(int id);

}
