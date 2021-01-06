package com.jSpaceProject.BeastProject.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotBlank;

import java.util.UUID;

public class Person {

    private UUID id;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    public Person(@JsonProperty("id") UUID id,
                  @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
