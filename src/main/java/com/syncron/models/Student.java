package com.syncron.models;

public class Student extends User {
    private boolean isCR; // Class Representative logic
    private double cgpa;
    private String section;

    public Student(String id, String name, String email, String password, boolean isCR) {
        super(id, name, email, password, "STUDENT"); // Passes data to the User (Parent) class
        this.isCR = isCR;
        this.cgpa = 0.0; // Default starts at 0
        this.section = "";
    }

    public Student(String id, String name, String email, String password, boolean isCR, String section) {
        this(id, name, email, password, isCR);
        this.section = section != null ? section : "";
    }

    public boolean isCR() { return isCR; }
    public void setCR(boolean CR) { isCR = CR; }

    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
}
