package model;

import java.util.UUID;

public class User {

    private String name;
    private UUID id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User(String name, UUID id) {
        this.name = name;
        this.id = id;
    }
}
