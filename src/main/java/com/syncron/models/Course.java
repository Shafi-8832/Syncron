package com.syncron.models;

import java.util.ArrayList;

public class Course {
    private String courseCode; // e.g., "CSE 108"
    private String courseTitle;
    private String credits;
    private String type;
    private ArrayList<Teacher> instructors; // Changed from single Teacher to a List
    private ArrayList<Student> enrolledStudents;

    public Course(String courseCode, String courseTitle, String credits, String type) {
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.credits = credits;
        this.type = type;
    }

    public void addTeacher(Teacher t) {
        instructors.add(t);
    }

    public void addStudent(Student s) {
        enrolledStudents.add(s);
    }

    // Getters
    public String getCourseCode() { return courseCode; }
    public String getCourseTitle() { return courseTitle; }

    // Returns the full list of teachers
    public ArrayList<Teacher> getInstructors() { return instructors; }

    public ArrayList<Student> getEnrolledStudents() { return enrolledStudents; }

}