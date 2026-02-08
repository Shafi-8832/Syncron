package com.syncron.models;

public class Teacher extends User {
    private String designation; // e.g., "Lecturer", "Professor"

    public Teacher(String id, String name, String email, String password, String designation) {
        super(id, name, email, password);
        this.designation = designation;
    }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
}