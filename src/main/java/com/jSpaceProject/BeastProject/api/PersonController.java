package com.jSpaceProject.BeastProject.api;

import com.jSpaceProject.BeastProject.model.Person;
import com.jSpaceProject.BeastProject.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
    public void addPerson(@RequestBody Person person) {
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

    @DeleteMapping
    public void snap(){
        System.out.println("er her");
    }


    @DeleteMapping(path = "{id}")
    public void deletePersonById(@PathVariable("id") UUID id){
        personService.deletePerson(id);
    }

    @PutMapping(path = {"id"})
    public void updatePersonById(@PathVariable("id") UUID id, @RequestBody Person person){
        System.out.println("er her");
        System.out.println("er her");
        personService.updatePerson(id, person);
    }

}
