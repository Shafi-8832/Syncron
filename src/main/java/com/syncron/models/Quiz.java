package com.syncron.models;

public class Quiz extends Assessment {
    private String duration;

    public Quiz(int id, String courseCode, int weekNumber, String title, String dateTime, String room, String duration) {
        super(id, courseCode, weekNumber, title, dateTime, room, "Quiz");
        this.duration = duration;
    }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
}
