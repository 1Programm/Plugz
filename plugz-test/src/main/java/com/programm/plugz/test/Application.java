package com.programm.plugz.test;

import com.programm.plugz.api.auto.Get;
import com.programm.plugz.magic.MagicEnvironment;
import com.programm.plugz.webserv.api.GetMapping;
import com.programm.plugz.webserv.api.PostMapping;
import com.programm.plugz.webserv.api.RequestBody;
import com.programm.plugz.webserv.api.RestController;

import java.util.List;

@RestController("/person")
public class Application {

    public static void main(String[] args) throws Exception {
        MagicEnvironment.Start(args);
    }

    @GetMapping("/all")
    public List<Person> getAll(@Get PersonRepo personRepo){
        return personRepo.findAll();
    }

    @GetMapping("/gen")
    public String generateRandomPerson(@Get PersonRepo personRepo){
        Person person = new Person();
        person.name = "Test Person";
        person.age = (int)(Math.random() * 99 + 1);

        personRepo.save(person);

        return "Generated a new Person";
    }

    @PostMapping("/save")
    public void savePerson(@Get PersonRepo personRepo, @RequestBody Person person){
        personRepo.save(person);
    }

    @PostMapping("/delete")
    public void deletePerson(@Get PersonRepo personRepo, @RequestBody Person person){
        personRepo.delete(person);
    }

}
