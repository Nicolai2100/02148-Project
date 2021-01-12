package model;

import java.util.UUID;

public class User {

    public User(String name, UUID id) {
        this.name = name;
        this.id = id;
    }

    public User(String name, UUID id, Account account) {
        this.name = name;
        this.id = id;
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    private String name;
    private UUID id;
    private Account account;


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
}
