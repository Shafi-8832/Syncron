package com.syncron.models;

public class Module {
    private int id;
    private String type;
    private String title; // resources?offline?online?
    private String description;
    private String fileLink;
    private String dueDate;

    public Module(int id, String type, String title, String description, String fileLink, String dueDate) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.fileLink = fileLink;
        this.dueDate = dueDate;
    }

    // UI will read data
    // Getters for the UI to read data
    public String getTitle() {return title;}
    public String getType() {return type;}
    public String getDescription() { return description; }
    public String getFileLink() { return fileLink; }

    public String getDueDate() {
        return dueDate;
    }
    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

}
