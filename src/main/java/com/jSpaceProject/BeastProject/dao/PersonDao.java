package com.jSpaceProject.BeastProject.dao;

import com.jSpaceProject.BeastProject.model.Person;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonDao {

    int insertPerson(UUID id, Person person);

    default int insertPerson(Person person){
        UUID id = UUID.randomUUID();
        return insertPerson(id, person);
    }

    List<Person> selectAllPeople();

    Optional<Person> selectPersonById(UUID id);

    int delete(UUID id);

    int update(UUID id, Person person);
}
