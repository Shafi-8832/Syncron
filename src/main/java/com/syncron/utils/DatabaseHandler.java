package com.syncron.utils;

import com.syncron.models.Course;
import javafx.collections.ObservableList;

import com.syncron.models.Section;
import com.syncron.models.Module;

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

        // --- NEW TABLES FOR VERTICAL PORTAL ---

        // 1. Sections (The "Weeks")
        String sqlSections = "CREATE TABLE IF NOT EXISTS course_sections (" +
                "section_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "course_code TEXT, " +     // Links to courses(code)
                "title TEXT, " +           // e.g. "Week 1"
                "week_number INTEGER, " +
                "flair_type TEXT, " +      // 'project', 'exam'
                "FOREIGN KEY(course_code) REFERENCES courses(code)" +
                ");";

        // 2. Modules (The "Items" inside weeks)
        String sqlModules = "CREATE TABLE IF NOT EXISTS course_modules (" +
                "module_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "section_id INTEGER, " +
                "module_type TEXT, " +     // 'resource', 'offline'
                "title TEXT, " +
                "description TEXT, " +
                "file_link TEXT, " +
                "is_active INTEGER DEFAULT 1, " +
                "due_date TEXT, " +
                "FOREIGN KEY(section_id) REFERENCES course_sections(section_id)" +
                ");";


        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlStudent);
            stmt.execute(sqlCourse);
            stmt.execute(sqlEnrollment);

            // Execute new tables
            stmt.execute(sqlSections);
            stmt.execute(sqlModules);

            System.out.println("Database initialized: All Tables created.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // --- NEW METHOD: The "Ghost Teacher" Data ---
    public static void addGhostTeacherData() {
        String sqlSection = "INSERT INTO course_sections (course_code, title, week_number, flair_type) VALUES (?, ?, ?, ?)";
        String sqlModule  = "INSERT INTO course_modules (section_id, module_type, title, description, due_date) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement psSection = conn.prepareStatement(sqlSection, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psModule = conn.prepareStatement(sqlModule)) {

            // ==========================================
            // 1. CREATE "GENERAL RESOURCES" (Week 0)
            // ==========================================
            psSection.setString(1, "CSE 108");
            psSection.setString(2, "General Resources");
            psSection.setInt(3, 0);
            psSection.setString(4, "none");
            psSection.executeUpdate();

            int commonSectionId = psSection.getGeneratedKeys().getInt(1);

            // Add Item 1: Syllabus (Resource)
            psModule.setInt(1, commonSectionId);
            psModule.setString(2, "resource");
            psModule.setString(3, "Course Syllabus.pdf");
            psModule.setString(4, "Mark distribution inside.");
            psModule.executeUpdate();

            // Add Item 2: Teacher Contact Info (Resource)
            psModule.setInt(1, commonSectionId);
            psModule.setString(2, "resource");
            psModule.setString(3, "Faculty Office Hours.txt");
            psModule.setString(4, "Room 305");
            psModule.executeUpdate();

            // ==========================================
            // 2. CREATE "HELL WEEK" (Week 12)
            // ==========================================
            psSection.setString(1, "CSE 108");
            psSection.setString(2, "Week 12: Final Project");
            psSection.setInt(3, 12);
            psSection.setString(4, "project"); // The Red Flair!
            psSection.executeUpdate();

            int projectSectionId = psSection.getGeneratedKeys().getInt(1);

            // Add Item 1: Project Guidelines (Resource)
            psModule.setInt(1, projectSectionId);
            psModule.setString(2, "resource");
            psModule.setString(3, "Project_Guidelines_v2.pdf");
            psModule.setString(4, "Read this before starting.");
            psModule.executeUpdate();

            // Add Item 2: Phase 1 Code Submission (Offline)
            psModule.setInt(1, projectSectionId);
            psModule.setString(2, "offline");
            psModule.setString(3, "Submit Phase 1 (JavaFX)");
            psModule.setString(4, "Upload .zip only.");
            psModule.executeUpdate();

            // Add Item 3: Phase 2 Database Submission (Offline)
            psModule.setInt(1, projectSectionId);
            psModule.setString(2, "offline");
            psModule.setString(3, "Submit Phase 2 (SQLite DB)");
            psModule.setString(4, "Upload your .db file.");
            psModule.executeUpdate();

            System.out.println("Ghost Teacher data added! (Packed with content)");

        } catch (SQLException e) {
            System.out.println("Ghost Data Error: " + e.getMessage());
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
    public static ObservableList<Course> getAllCourses() {

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

    public static List<Section> getSectionsForCourse(String courseCode) {
        List<com.syncron.models.Section> sectionList = new ArrayList<>();

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

}