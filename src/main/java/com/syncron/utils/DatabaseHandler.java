package com.syncron.utils;

import com.syncron.models.Course;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {

    // This creates a file named 'syncron.db' in your project folder
    private static final String DB_URL = "jdbc:sqlite:syncron.db";

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to SQLite.");
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
        return conn;
    }

    // Run this method ONCE to create your tables
    public static void initializeDB() {
        // SQL to create a Student table
        String studentsSql = "CREATE TABLE IF NOT EXISTS students ("
                + "id TEXT PRIMARY KEY, "
                + "name TEXT NOT NULL, "
                + "password TEXT NOT NULL);";

        // SQL to create a Courses table
        String coursesSql = "CREATE TABLE IF NOT EXISTS courses ("
                + "course_code TEXT PRIMARY KEY, "
                + "course_title TEXT NOT NULL);";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(studentsSql);
            stmt.execute(coursesSql);
            System.out.println("Database initialized.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Returns a list of dummy courses for now
    public static List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();
        courses.add(new Course("CSE 108", "Object Oriented Programming"));
        courses.add(new Course("CSE 203", "Data Structures"));
        courses.add(new Course("CSE 305", "Database Systems"));
        courses.add(new Course("CSE 401", "Computer Networks"));
        courses.add(new Course("MAT 101", "Calculus I"));
        return courses;
    }
}