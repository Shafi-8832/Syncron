package com.syncron.models;

public abstract class User {
    private String id;
    private String name;
    private String email;
    private String password;

    // Constructor
    public User(String id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    // Logic
    public boolean checkPassword(String inputPassword) {
        return this.password.equals(inputPassword);
    }
}