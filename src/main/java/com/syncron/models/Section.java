package com.syncron.models;

import java.util.ArrayList;
import java.util.List;

public class Section {

    // Section == Week

    private int id;
    private String title; // " Week 1 "
    private int weekNumber;
    private String flairType;
    private List<Module> modules; // The list of items inside this week

    private int startDate; // 1 january - 7 january
    private int labDay; // Monday 3 January

    public Section(int id, String title, int weekNumber, String flairType) {
        this.id = id;
        this.title = title;
        this.weekNumber = weekNumber;
        this.flairType = flairType;
        this.modules = new ArrayList<>(); // Start with an empty list
    }


    public String getTitle() {return title;}
    public String getFlairType() {return flairType;}
    public List<Module> getModules() {return modules;}

    // Helper to push_back items
    public void addModule(Module m) {
        this.modules.add(m);
    }
}
