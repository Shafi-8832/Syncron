package com.syncron.models;

public abstract class Assessment {
    private int id;
    private String courseCode;
    private int weekNumber;
    private String title;
    private String dateTime;
    private String room;
    private String assessmentType;

    public Assessment(int id, String courseCode, int weekNumber, String title, String dateTime, String room, String assessmentType) {
        this.id = id;
        this.courseCode = courseCode;
        this.weekNumber = weekNumber;
        this.title = title;
        this.dateTime = dateTime;
        this.room = room;
        this.assessmentType = assessmentType;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public int getWeekNumber() { return weekNumber; }
    public void setWeekNumber(int weekNumber) { this.weekNumber = weekNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getAssessmentType() { return assessmentType; }
}
