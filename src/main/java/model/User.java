package model;

import java.util.UUID;

public class User {

    private String name;
    private String password;
    private UUID id;
    private Account account;

    public User(String name, UUID id) {
        this.name = name;
        this.id = id;
    }

    public User(String name, UUID id, Account account) {
        this.name = name;
        this.id = id;
        this.account = account;
    }

    public User(String name, UUID id, String password, Account account) {
        this.name = name;
        this.id = id;
        this.account = account;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

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
