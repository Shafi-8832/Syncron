package com.syncron.models;

public class Module {
    private int id;
    private String type;
    private String title; // resources?offline?online?
    private String description;
    private String fileLink;

    public Module(int id, String type, String title, String description, String fileLink) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.fileLink = fileLink;
    }

    // UI will read data
    // Getters for the UI to read data
    public String getTitle() {return title;}
    public String getType() {return type;}
    public String getDescription() { return description; }
    public String getFileLink() { return fileLink; }
}
