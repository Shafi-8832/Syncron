package com.syncron.models;

import java.time.LocalDate;

public class Assignment {
    private String title;
    private String description;
    private LocalDate deadline;
    private double maxMarks;

    public Assignment(String title, String description, LocalDate deadline, double maxMarks) {
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.maxMarks = maxMarks;
    }

    // Getters
    public String getTitle() { return title; }
    public LocalDate getDeadline() { return deadline; }
    public double getMaxMarks() { return maxMarks; }
}