package com.jSpaceProject.BeastProject.service;

import com.jSpaceProject.BeastProject.dao.PersonDao;
import com.jSpaceProject.BeastProject.model.Person;

public class PersonService {
    private final PersonDao personDao;

    public PersonService(PersonDao personDao, int addPerson) {
        this.personDao = personDao;
    }

    public int addPerson(Person person) {
        return personDao.insertPerson(person);
    }
}
