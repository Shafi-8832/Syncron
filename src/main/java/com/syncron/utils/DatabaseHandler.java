package com.syncron.utils;

import com.syncron.models.Course;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.syncron.models.Section;
import com.syncron.models.Module;
import com.syncron.models.Assessment;
import com.syncron.models.CT;
import com.syncron.models.Assignment;
import com.syncron.models.Offline;
import com.syncron.models.Online;
import com.syncron.models.Quiz;

import javax.naming.spi.ResolveResult;
import java.sql.*;
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

        // --- RBAC CORE TABLES ---

        String sqlUsers = "CREATE TABLE IF NOT EXISTS users ("
                + "id TEXT PRIMARY KEY, "
                + "name TEXT, "
                + "email TEXT UNIQUE, "
                + "password TEXT, "
                + "role TEXT);";

        String sqlSemesters = "CREATE TABLE IF NOT EXISTS semesters ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT, "
                + "is_active INTEGER DEFAULT 1);";

        String sqlCourses = "CREATE TABLE IF NOT EXISTS courses ("
                + "code TEXT PRIMARY KEY, "
                + "title TEXT, "
                + "course_type TEXT, "
                + "semester_id INTEGER, "
                + "FOREIGN KEY(semester_id) REFERENCES semesters(id));";

        String sqlCourseTeachers = "CREATE TABLE IF NOT EXISTS course_teachers ("
                + "teacher_id TEXT, "
                + "course_code TEXT, "
                + "PRIMARY KEY (teacher_id, course_code));";

        String sqlEnrollmentRequests = "CREATE TABLE IF NOT EXISTS enrollment_requests ("
                + "student_id TEXT, "
                + "course_code TEXT, "
                + "status TEXT DEFAULT 'PENDING', "
                + "PRIMARY KEY (student_id, course_code));";

        // --- EXISTING TABLES ---

        // 1. Sections (The "Weeks")
        String sqlSections = "CREATE TABLE IF NOT EXISTS course_sections (" +
                "section_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "course_code TEXT, " +
                "title TEXT, " +
                "week_number INTEGER, " +
                "flair_type TEXT, " +
                "FOREIGN KEY(course_code) REFERENCES courses(code)" +
                ");";

        // 2. Modules (The "Items" inside weeks)
        String sqlModules = "CREATE TABLE IF NOT EXISTS course_modules (" +
                "module_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "section_id INTEGER, " +
                "module_type TEXT, " +
                "title TEXT, " +
                "description TEXT, " +
                "file_link TEXT, " +
                "is_active INTEGER DEFAULT 1, " +
                "due_date TEXT, " +
                "FOREIGN KEY(section_id) REFERENCES course_sections(section_id)" +
                ");";

        // 3. Assessments (Single Table Inheritance for CT, Assignment, Offline, Online, Quiz)
        String sqlAssessments = "CREATE TABLE IF NOT EXISTS assessments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "course_code TEXT, " +
                "week_number INTEGER, " +
                "assessment_type TEXT, " +
                "title TEXT, " +
                "date_time TEXT, " +
                "room TEXT, " +
                "duration TEXT, " +
                "total_marks INTEGER, " +
                "syllabus TEXT, " +
                "submission_link TEXT, " +
                "author_name TEXT, " +
                "FOREIGN KEY(course_code) REFERENCES courses(code)" +
                ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlUsers);
            stmt.execute(sqlSemesters);
            stmt.execute(sqlCourses);
            stmt.execute(sqlCourseTeachers);
            stmt.execute(sqlEnrollmentRequests);

            stmt.execute(sqlSections);
            stmt.execute(sqlModules);
            stmt.execute(sqlAssessments);

            System.out.println("Database initialized: All Tables created.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void injectDefaultAdmin() {
        String sqlAdmin = "INSERT OR IGNORE INTO Users (id, name, email, password, role) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sqlAdmin)) {

            pstmt.setString(1, "Admin");
            pstmt.setString(2, "IICT Admin");
            pstmt.setString(3, "admin@iict.buet.ac.bd");
            pstmt.setString(4, "admin69");
            pstmt.setString(5, "ADMIN");

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) System.out.println("default admin has been successfully entered into the database");
            else System.out.println("Admin already exists. Skipped creation.");
        }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    // New Method: Fetch all courses from the database
    // "ObservableList" is a special JavaFX list that automatically updates the UI when data changes.
    public static ObservableList<Course> getAllCourses() {

        // 1. Create an empty list to hold the courses
        ObservableList<Course> courseList = FXCollections.observableArrayList();

        String sql = "SELECT * FROM courses"; // The question we ask the DB

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) { // executeQuery is for READING data

            // 2. The Loop: "While there is a next row..."
            while (rs.next()) {
                // 3. Read the columns
                String code = rs.getString("code");
                String title = rs.getString("title");

                // 4. Create a Course object and add it to the list
                courseList.add(new Course(code, title));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return courseList; // Return the full list
    }

    public static List<Section> getSectionsForCourse(String courseCode) {
        List<Section> sectionList = new ArrayList<>();

        // SQL : get all weeks, 0, 1, 2, ...
        String sql = "SELECT * FROM course_sections WHERE course_code = ? ORDER BY  week_number ASC";

        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, courseCode);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // 1. Create the Section Object (The "Week Box")
                int sectionId = rs.getInt("section_id");
                String title = rs.getString("title");
                int weekNum = rs.getInt("week_number");
                String flair = rs.getString("flair_type");

                Section newSection = new Section(sectionId, title, weekNum, flair);

                // 2. CRITICAL STEP : Fetch the items inside this specific section
                // We call the helper method
                List<Module> items = getModulesForSection(sectionId);

                // 3. Add all items to the section
                for (Module m : items) {
                    newSection.addModule(m);
                }

                // 4. Add the finished section (week) to our main list
                sectionList.add(newSection);
            }
        } catch (SQLException e) {
            System.out.println("Error fetching sections : " + e.getMessage());
        }

        return sectionList;
    }

    private static List<Module> getModulesForSection(int sectionId) {
        List<Module> moduleList = new ArrayList<>();
        String sql = "SELECT * FROM course_modules WHERE section_id = ?";

        try (Connection conn = connect();
        PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setInt(1, sectionId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                moduleList.add(new Module(
                        rs.getInt("module_id"),
                        rs.getString("module_type"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("file_link"),
                        rs.getString("due_date")
                ));
            }
        }
        catch (SQLException e) {
            System.out.println("Error fetching modules: " + e.getMessage());
        }

        return moduleList;
    }

    public static List<Assessment> getAssessmentsForCourse(String courseCode) {
        List<Assessment> assessmentList = new ArrayList<>();

        String sql = "SELECT * FROM assessments WHERE course_code = ? ORDER BY week_number ASC";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String code = rs.getString("course_code");
                int weekNumber = rs.getInt("week_number");
                String title = rs.getString("title");
                String dateTime = rs.getString("date_time");
                String room = rs.getString("room");
                String assessmentType = rs.getString("assessment_type");

                switch (assessmentType) {
                    case "CT":
                        String syllabus = rs.getString("syllabus");
                        int totalMarks = rs.getInt("total_marks");
                        assessmentList.add(new CT(id, code, weekNumber, title, dateTime, room, syllabus, totalMarks));
                        break;
                    case "Assignment":
                        String assignmentLink = rs.getString("submission_link");
                        assessmentList.add(new Assignment(id, code, weekNumber, title, dateTime, room, assignmentLink));
                        break;
                    case "Offline":
                        String offlineLink = rs.getString("submission_link");
                        assessmentList.add(new Offline(id, code, weekNumber, title, dateTime, room, offlineLink));
                        break;
                    case "Online":
                        String onlineDuration = rs.getString("duration");
                        assessmentList.add(new Online(id, code, weekNumber, title, dateTime, room, onlineDuration));
                        break;
                    case "Quiz":
                        String quizDuration = rs.getString("duration");
                        assessmentList.add(new Quiz(id, code, weekNumber, title, dateTime, room, quizDuration));
                        break;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching assessments: " + e.getMessage());
        }

        return assessmentList;
    }

    public static List<Module> getUpcomingDeadlines() throws SQLException {
        List<Module> urgentList = new ArrayList<>();

        // SQL : Find items where due_date is between TODAY and 7 DAYS from now
        String sql = "SELECT * FROM course_modules " +
                "WHERE due_date BETWEEN date('now') AND date('now', '+7 days') " +
                "ORDER BY due_date ASC";

        try (Connection conn = connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {


            while (rs.next()) {
                Module module = new Module(
                        rs.getInt("module_id"),
                        rs.getString("title"),
                        rs.getString("module_type"),
                        rs.getString("description"),
                        rs.getString("file_link"),
                        rs.getString("due_date")
                );
                urgentList.add(module);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return urgentList;
    }

    /**
     * Authenticates a user and returns their RBAC role.
     * @param id The Student ID, Teacher ID, or Admin ID
     * @param inputPassword The password typed into the UI
     * @return The user's role (e.g., "ADMIN", "TEACHER", "STUDENT"), or null if login fails.
     */
    public static String authenticateUser(String id, String inputPassword) {
        String sqlCheck = "SELECT password, role FROM users WHERE id = ?";

        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sqlCheck)) {

            pstmt.setString(1, id);

            // 4. execute the query and receive java's spreadsheet object ResultSet
            try (ResultSet rs = pstmt.executeQuery()) {

                // 5. Does this user exist? Move to the first row of data.
                if (rs.next()) { // step down to the first row from header row
                    // Grab the real password and role from database row
                    String dbPassword = rs.getString("password");
                    String dbRole = rs.getString("role");

                    // 6. verification
                    if (dbPassword.equals(inputPassword)) {
                        System.out.println("Login successful for user " + id);
                        return dbRole;
                    }
                    else {
                        System.out.println("Login failed: Incorrect password for user: " + id);
                        return null;
                    }
                }
                else {
                    System.out.println("Login failed: ID not found.");
                    return null; // User with this ID doesn't exist at all.
                }

            }
        } catch (SQLException e) {
            System.out.println("Database error during authentication: " + e.getMessage());
            return null; // Connection failed or Something else crashed.
        }


    }
}