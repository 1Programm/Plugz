package com.programm.projects.plugz.test;

import com.programm.projects.plugz.magic.api.db.ICrudRepo;
import com.programm.projects.plugz.magic.api.db.Repo;

import java.util.List;

@Repo
public interface UserRepo extends ICrudRepo<Integer, TestUser> {

    List<TestUser> findByName(String name);

    List<TestUser> findByIdAndName(int id, String name);

}
