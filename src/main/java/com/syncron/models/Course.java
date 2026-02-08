package com.syncron.models;

import java.util.ArrayList;

public class Course {
    private String courseCode; // e.g., "CSE 108"
    private String courseTitle;
    private Teacher instructor;
    private ArrayList<Student> enrolledStudents;

    public Course(String courseCode, String courseTitle, Teacher instructor) {
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.instructor = instructor;
        this.enrolledStudents = new ArrayList<>();
    }

    public void addStudent(Student s) {
        enrolledStudents.add(s);
    }

    // Getters
    public String getCourseCode() { return courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public Teacher getInstructor() { return instructor; }
    public ArrayList<Student> getEnrolledStudents() { return enrolledStudents; }
}