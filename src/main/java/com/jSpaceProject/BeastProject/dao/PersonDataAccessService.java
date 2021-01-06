package com.jSpaceProject.BeastProject.dao;

import com.jSpaceProject.BeastProject.model.Person;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("postgres")
public class PersonDataAccessService implements PersonDao{
    @Override
    public int insertPerson(UUID id, Person person) {
        return 0;
    }

    @Override
    public List<Person> selectAllPeople() {
        return List.of(new Person(UUID.randomUUID(), "From postgress DB"));
    }

    @Override
    public Optional<Person> selectPersonById(UUID id) {
        return Optional.empty();
    }

    @Override
    public int delete(UUID id) {
        return 0;
    }

    @Override
    public int update(UUID id, Person person) {
        return 0;
    }
}
