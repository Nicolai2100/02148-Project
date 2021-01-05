package com.jSpaceProject.BeastProject.dao;

import com.jSpaceProject.BeastProject.model.Person;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

//Insta at beginning
@Repository("fakeDao")
public class FakePersonDataAccessService implements PersonDao{
    private static List<Person> DB = new ArrayList<>();

    @Override
    public int insertPerson(UUID id, Person person) {
        DB.add(new Person(id, person.getName()));
        return 1;
    }

    @Override
    public List<Person> selectAllPeople() {
        return DB;
    }

    @Override
    public Optional<Person> selectPersonById(UUID id) {
        return DB.stream()
                .filter(person -> person.getId().equals(id))
                .findFirst();
    }

    @Override
    public int delete(UUID id) {
        Optional<Person> personMayby = selectPersonById(id);
        if (personMayby.isEmpty())
                return 0;
        else
            DB.remove(personMayby.get());
        return 0;
    }

    @Override
    public int update(UUID id, Person person) {
        return 0;
    }

}
