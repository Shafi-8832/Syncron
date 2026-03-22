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

    public String getSubsection() {
        try {
            String idStr = this.getId();

            if (idStr == null || idStr.length() < 3) return "--";

            int roll = Integer.parseInt(idStr.substring(idStr.length() - 3));

            if (roll >= 1 && roll <= 30) return "A1";
            if (roll >= 31 && roll <= 60) return "A2";
            if (roll >= 61 && roll <= 90) return "B1";
            if (roll >= 91 && roll <= 120) return "B2";
            if (roll >= 121 && roll <= 150) return "C1";
            if (roll >= 151 && roll <= 181) return "C2";

        } catch (NumberFormatException e) {
            return "--";
        }

        return "--"; // Fallback if roll is somehow > 181 or <= 0
    }}
