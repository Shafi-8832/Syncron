package com.syncron.models;

public class Offline extends Assessment {
    private String submissionLink;

    public Offline(int id, String courseCode, int weekNumber, String title, String dateTime, String room, String submissionLink) {
        super(id, courseCode, weekNumber, title, dateTime, room, "Offline");
        this.submissionLink = submissionLink;
    }

    public String getSubmissionLink() { return submissionLink; }
    public void setSubmissionLink(String submissionLink) { this.submissionLink = submissionLink; }
}
