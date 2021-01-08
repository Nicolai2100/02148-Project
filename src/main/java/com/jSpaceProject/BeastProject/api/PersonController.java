package com.jSpaceProject.BeastProject.api;

import com.jSpaceProject.BeastProject.model.Person;
import com.jSpaceProject.BeastProject.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/person")
//Rest controller
@RestController
public class PersonController {
    private final PersonService personService;

    //Dependency Injection
    @Autowired
    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    //Post request
    @PostMapping //Take request body and shove into person - turns json object into person object
    public void addPerson(@Valid @NonNull @RequestBody Person person) {
        personService.addPerson(person);
    }

    //Get request
    @GetMapping
    public List<Person> getAllPeople() {
        return personService.getAllPeople();
    }

    //Get request - with path "/some id"
    @GetMapping(path = "{id}")
    public Person getPerson(@PathVariable("id") UUID id) { //grab the is from the path and turn it into an uuid
        return personService.getPersonById(id)
                .orElse(null); //Throw 404 user not found
    }

    @PutMapping(path = "{id}")
    public void updatePersonById(@PathVariable("id") UUID id, @Valid @NonNull @RequestBody Person person) {
        personService.updatePerson(id, person);
    }

    @DeleteMapping(path = "{id}")
    public void deletePersonById(@PathVariable("id") UUID id) {
        personService.deletePerson(id);
    }
}
