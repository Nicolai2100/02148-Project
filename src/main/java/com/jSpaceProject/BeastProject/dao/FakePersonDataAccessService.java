package com.jSpaceProject.BeastProject.dao;

import com.jSpaceProject.BeastProject.model.Person;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

//Insta at beginning
@Repository("fakeDao")
public class FakePersonDataAccessService implements PersonDao {
    private static List<Person> DB;

    public FakePersonDataAccessService(){
        DB = new ArrayList<>();
        DB.add(new Person(new UUID(1,1), "James Bond"));
        DB.add(new Person(new UUID(1,2), "The Beast"));
        DB.add(new Person(new UUID(1,3), "Hulken"));
    }

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
        Optional<Person> personMaybe = selectPersonById(id);
        if (personMaybe.isEmpty())
            return 0;
        else
            DB.remove(personMaybe.get());
        return 1;
    }

    @Override
    public int update(UUID id, Person person) {
        return selectPersonById(id)
                .map(p -> {
                    int indexOfPersonToUpdate = DB.indexOf(p);
                    if (indexOfPersonToUpdate >= 0) {
                        DB.set(indexOfPersonToUpdate, new Person(id, person.getName()));
                        return 1;
                    }
                    return 0;
                }).orElse(0)
                ;
    }
}
