package com.programm.plugz.test;

import com.programm.plugz.api.Async;
import com.programm.plugz.api.Service;
import com.programm.plugz.api.auto.Get;
import com.programm.plugz.api.lifecycle.PostStartup;
import com.programm.plugz.magic.MagicEnvironment;

@Service
public class Application {

    public static void main(String[] args) throws Exception {
        MagicEnvironment.Start();
    }

    @PostStartup
    public void onStart(@Get PersonRepo repo, @Get TagRepo r2){
        Tag t = new Tag();
        t.setTitle("A");
        t.setDescription("B");

        r2.save(t);
//        t.setId(1);

        Person p = new Person();
        p.setName("Julian");
        p.setTag(t);
        repo.save(p);
        Person p2 = repo.findById(1);
        System.out.println("Person Tag: " + p2.getTag());
        System.out.println("ID: " + p2.getId() + ", Name: " + p2.getName());
        System.out.println("ID: " + p2.getId() + ", Name: " + p2.getName());
    }


}
