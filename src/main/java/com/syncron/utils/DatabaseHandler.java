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
import com.syncron.models.Student;
import com.syncron.models.Teacher;
import com.syncron.models.User;

import javax.naming.spi.ResolveResult;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class DatabaseHandler {

    // This creates a file named 'syncron.db' in your project folder
    private static final String DB_URL = "jdbc:sqlite:syncron.db";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PASSWORD_ITERATIONS = 120000;
    private static final int PASSWORD_KEY_LENGTH = 256;
    private static final int PASSWORD_SALT_BYTES = 16;
    private static final int MIN_PASSWORD_LENGTH = 8;

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
                    if (verifyPassword(dbPassword, inputPassword)) {
                        if (!dbPassword.startsWith("pbkdf2$")) {
                            migratePasswordHash(id, inputPassword);
                        }
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

    public static boolean updatePassword(String userId, String currentPass, String newPass) {
        String selectSql = "SELECT password FROM users WHERE id = ?";
        String updateSql = "UPDATE users SET password = ? WHERE id = ?";

        if (isBlank(userId) || isBlank(currentPass) || isBlank(newPass) || !isStrongPassword(newPass)) {
            return false;
        }

        try (Connection conn = connect();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            selectStmt.setString(1, userId);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }

                String dbPassword = rs.getString("password");
                if (!verifyPassword(dbPassword, currentPass)) {
                    return false;
                }
            }

            updateStmt.setString(1, hashPassword(newPass));
            updateStmt.setString(2, userId);
            return updateStmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error updating password.");
            return false;
        }
    }

    private static boolean isStrongPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean verifyPassword(String storedPassword, String providedPassword) {
        if (isBlank(storedPassword) || providedPassword == null) {
            return false;
        }

        if (!storedPassword.startsWith("pbkdf2$")) {
            return storedPassword.equals(providedPassword);
        }

        String[] parts = storedPassword.split("\\$");
        if (parts.length != 4) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] candidateHash = pbkdf2(providedPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
            return MessageDigest.isEqual(expectedHash, candidateHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String hashPassword(String password) {
        byte[] salt = new byte[PASSWORD_SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, PASSWORD_ITERATIONS, PASSWORD_KEY_LENGTH);
        return "pbkdf2$" + PASSWORD_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed.", e);
        }
    }

    private static void migratePasswordHash(String userId, String plainPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hashPassword(plainPassword));
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Password migration failed for user " + userId + ".");
        }
    }

    public static String getUserNameById(String id) {
        String sql = "SELECT name FROM users WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while fetching user name: " + e.getMessage());
        }

        return "";
    }


    public static String injectLevel1Term2() {
        String sqlSemester = "INSERT INTO semesters (name, is_active) VALUES ('Level 1 Term 2', 1)";
        String sqlCourse = "INSERT INTO courses (code, title, course_type, semester_id) VALUES (?, ?, 'THEORY', ?)";
        String sqlUsers = "INSERT OR IGNORE INTO users (id, name, email, password, role) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement psSem = conn.prepareStatement(sqlSemester, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psCourse = conn.prepareStatement(sqlCourse);
             PreparedStatement psUser = conn.prepareStatement(sqlUsers)) {

            // 1. Create Level 1 Term 2
            psSem.executeUpdate();
            ResultSet rs = psSem.getGeneratedKeys();
            int semesterId = rs.next() ? rs.getInt(1) : 1;

            // 2. Inject Courses
            String[][] courses = {
                    {"CSE 105", "Data Structures and Algorithms"},
                    {"CSE 107", "Object-Oriented Programming"},
                    {"ME 174", "Mechanical Drawing"}
            };

            for (String[] course : courses) {
                psCourse.setString(1, course[0]);
                psCourse.setString(2, course[1]);
                psCourse.setInt(3, semesterId);
                psCourse.executeUpdate();
            }

            // 3. Create Test Student & Teacher
            psUser.setString(1, "student"); psUser.setString(2, "Test Student"); psUser.setString(3, "student@buet.ac.bd"); psUser.setString(4, "1234"); psUser.setString(5, "STUDENT");
            psUser.executeUpdate();

            psUser.setString(1, "teacher"); psUser.setString(2, "Test Teacher"); psUser.setString(3, "teacher@buet.ac.bd"); psUser.setString(4, "1234"); psUser.setString(5, "TEACHER");
            psUser.executeUpdate();

            return "SUCCESS: Level 1 Term 2 initialized. Courses (CSE 105, CSE 107, ME 174) and test users created.";

        } catch (SQLException e) {
            return "ERR: Injection failed - " + e.getMessage();
        }
    }

    public static List<User> getCourseParticipants(String courseCode) {
        List<User> participants = new ArrayList<>();

        String sql = """
                SELECT DISTINCT u.id,
                                u.name,
                                u.email,
                                u.password,
                                u.role,
                                '' AS section
                FROM users u
                LEFT JOIN enrollment_requests er
                       ON er.student_id = u.id
                      AND er.course_code = ?
                      AND upper(COALESCE(er.status, '')) = 'APPROVED'
                LEFT JOIN course_teachers ct
                       ON ct.teacher_id = u.id
                      AND ct.course_code = ?
                WHERE er.student_id IS NOT NULL
                   OR ct.teacher_id IS NOT NULL
                ORDER BY u.name COLLATE NOCASE ASC
                """;

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);
            pstmt.setString(2, courseCode);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    String password = rs.getString("password");
                    String role = rs.getString("role");
                    String normalizedRole = role == null ? "" : role.toUpperCase();

                    if ("STUDENT".equals(normalizedRole)) {
                        participants.add(new Student(
                                id,
                                name,
                                email,
                                password,
                                false,
                                rs.getString("section")
                        ));
                    } else if ("TEACHER".equals(normalizedRole)) {
                        participants.add(new Teacher(id, name, email, password, "Teacher"));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching course participants: " + e.getMessage());
        }

        return participants;
    }

}
