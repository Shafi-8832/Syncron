package com.syncron.models;

public class Student extends User {
    private boolean isCR; // Class Representative logic
    private double cgpa;

    public Student(String id, String name, String email, String password, boolean isCR) {
        super(id, name, email, password); // Passes data to the User (Parent) class
        this.isCR = isCR;
        this.cgpa = 0.0; // Default starts at 0
    }

    public boolean isCR() { return isCR; }
    public void setCR(boolean CR) { isCR = CR; }

    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }
}