package com.syncron.models;

public class CT extends Assessment {
    private String syllabus;
    private int totalMarks;

    public CT(int id, String courseCode, int weekNumber, String title, String dateTime, String room, String syllabus, int totalMarks) {
        super(id, courseCode, weekNumber, title, dateTime, room, "CT");
        this.syllabus = syllabus;
        this.totalMarks = totalMarks;
    }

    public String getSyllabus() { return syllabus; }
    public void setSyllabus(String syllabus) { this.syllabus = syllabus; }

    public int getTotalMarks() { return totalMarks; }
    public void setTotalMarks(int totalMarks) { this.totalMarks = totalMarks; }
}
