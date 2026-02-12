package com.syncron.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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

        String sqlStudent = "CREATE TABLE IF NOT EXISTS students ("
                + "id TEXT PRIMARY KEY, "
                + "name TEXT NOT NULL, "
                + "password TEXT NOT NULL);";


        String sqlCourse = "CREATE TABLE IF NOT EXISTS courses ("
                + "code TEXT PRIMARY KEY, " // e.g., 'CSE 108' (Must be unique)
                + "title TEXT NOT NULL);";  // e.g., 'Object Oriented Programming'


        // This remembers which student is taking which course.
        String sqlEnrollment = "CREATE TABLE IF NOT EXISTS enrollments ("
                + "studentId TEXT, "
                + "courseCode TEXT, "
                + "PRIMARY KEY (studentId, courseCode));";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlStudent);
            stmt.execute(sqlCourse);
            stmt.execute(sqlEnrollment);

            System.out.println("Database initialized: Tables created.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    // A helper method to put fake data in so we can test the app
    public static void addSampleData() {
        String sql1 = "INSERT OR IGNORE INTO courses(code, title) VALUES('CSE 108', 'Object Oriented Programming');";
        String sql2 = "INSERT OR IGNORE INTO courses(code, title) VALUES('CSE 105', 'Data Structures');";
        String sql3 = "INSERT OR IGNORE INTO courses(code, title) VALUES('HUM 103', 'Economics');";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql1);
            stmt.execute(sql2);
            stmt.execute(sql3);

            System.out.println("Sample courses added.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    // New Method: Fetch all courses from the database
    // "ObservableList" is a special JavaFX list that automatically updates the UI when data changes.
    public static javafx.collections.ObservableList<com.syncron.models.Course> getAllCourses() {

        // 1. Create an empty list to hold the courses
        javafx.collections.ObservableList<com.syncron.models.Course> courseList = javafx.collections.FXCollections.observableArrayList();

        String sql = "SELECT * FROM courses"; // The question we ask the DB

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) { // executeQuery is for READING data

            // 2. The Loop: "While there is a next row..."
            while (rs.next()) {
                // 3. Read the columns
                String code = rs.getString("code");
                String title = rs.getString("title");

                // 4. Create a Course object and add it to the list
                courseList.add(new com.syncron.models.Course(code, title));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return courseList; // Return the full list
    }
}