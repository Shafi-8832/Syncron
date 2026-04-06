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

        String sql = "SELECT course_code, course_title, credits, type FROM courses"; // The question we ask the DB

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) { // executeQuery is for READING data

            // 2. The Loop: "While there is a next row..."
            while (rs.next()) {
                // 3. Read the columns
                String code = rs.getString("course_code");
                String title = rs.getString("course_title");
                String credits = rs.getString("credits");
                String type = rs.getString("type");

                // 4. Create a Course object and add it to the list
                courseList.add(new Course(code, title, credits, type));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return courseList; // Return the full list
    }

    public static List<Section> getSectionsForCourse(String courseCode) {
        List<Section> sectionList = new ArrayList<>();

        try {
            String safeCode = java.net.URLEncoder.encode(courseCode, StandardCharsets.UTF_8.toString());
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/sections/" + safeCode))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<java.util.Map<String, Object>>>(){}.getType();
                List<java.util.Map<String, Object>> dataList = gson.fromJson(response.body(), listType);

                for (java.util.Map<String, Object> data : dataList) {
                    int sectionId = ((Double) data.get("id")).intValue();
                    String title = String.valueOf(data.get("title"));
                    int weekNum = ((Double) data.get("weekNumber")).intValue();
                    String flair = String.valueOf(data.get("flairType"));

                    // 1. Create the Section
                    Section newSection = new Section(sectionId, title, weekNum, flair);

                    // 2. Extract and create the Modules!
                    List<java.util.Map<String, Object>> rawModules = (List<java.util.Map<String, Object>>) data.get("modules");
                    if (rawModules != null) {
                        for (java.util.Map<String, Object> mData : rawModules) {
                            Module newModule = new Module(
                                    ((Double) mData.get("id")).intValue(),
                                    String.valueOf(mData.get("type")),
                                    String.valueOf(mData.get("title")),
                                    mData.get("description") != null ? String.valueOf(mData.get("description")) : "",
                                    mData.get("fileLink") != null ? String.valueOf(mData.get("fileLink")) : "",
                                    mData.get("dueDate") != null ? String.valueOf(mData.get("dueDate")) : ""
                            );
                            newSection.addModule(newModule);
                        }
                    }

                    sectionList.add(newSection);
                }
            } else {
                System.out.println("❌ Failed to fetch sections. Status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.out.println("❌ Network error fetching sections.");
            e.printStackTrace();
        }

        return sectionList;
    }

    // Deleted!

    public static List<Assessment> getAssessmentsForCourse(String courseCode) {
        List<Assessment> assessmentList = new ArrayList<>();

        try {
            // Encode the space in the course code (e.g. "CSE 105" -> "CSE%20105") for the URL
            String safeCode = java.net.URLEncoder.encode(courseCode, StandardCharsets.UTF_8.toString());

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/assessments/" + safeCode))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<java.util.Map<String, Object>>>(){}.getType();
                List<java.util.Map<String, Object>> dataList = gson.fromJson(response.body(), listType);

                for (java.util.Map<String, Object> data : dataList) {
                    // Safely extract our data
                    int id = ((Double) data.get("id")).intValue(); // Gson reads numbers as Doubles by default
                    String code = String.valueOf(data.get("courseCode"));
                    int weekNumber = ((Double) data.get("weekNumber")).intValue();
                    String title = String.valueOf(data.get("title"));
                    String dateTime = String.valueOf(data.get("dateTime"));
                    String room = data.get("room") != null ? String.valueOf(data.get("room")) : "";
                    String assessmentType = String.valueOf(data.get("assessmentType"));

                    // Instantiate the correct object based on your model classes!
                    switch (assessmentType) {
                        case "CT":
                            String syllabus = String.valueOf(data.get("syllabus"));
                            int totalMarks = data.get("totalMarks") != null ? ((Double) data.get("totalMarks")).intValue() : 0;
                            assessmentList.add(new CT(id, code, weekNumber, title, dateTime, room, syllabus, totalMarks));
                            break;
                        case "Assignment":
                            String assignmentLink = String.valueOf(data.get("submissionLink"));
                            assessmentList.add(new Assignment(id, code, weekNumber, title, dateTime, room, assignmentLink));
                            break;
                        case "Offline":
                            String offlineLink = String.valueOf(data.get("submissionLink"));
                            assessmentList.add(new Offline(id, code, weekNumber, title, dateTime, room, offlineLink));
                            break;
                        case "Online":
                            String onlineDuration = String.valueOf(data.get("duration"));
                            assessmentList.add(new Online(id, code, weekNumber, title, dateTime, room, onlineDuration));
                            break;
                        case "Quiz":
                            String quizDuration = String.valueOf(data.get("duration"));
                            assessmentList.add(new Quiz(id, code, weekNumber, title, dateTime, room, quizDuration));
                            break;
                    }
                }
            } else {
                System.out.println("❌ Failed to fetch assessments. Status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.out.println("❌ Network error fetching assessments.");
            e.printStackTrace();
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

        // 1. Just reuse the method we already built
        participants.addAll(getTeachersByCourse(courseCode));

        // 2. Fetch the Students (Since this is a specific BUET batch, all students take these core courses)
        String studentQuery = "SELECT id, name, email, password, section, subsection FROM users WHERE role = 'STUDENT'";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(studentQuery)) {

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Student student = new com.syncron.models.Student(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        false,
                        rs.getString("section")
                );
                // student.setSubsection(rs.getString("subsection")); // Uncomment if your model supports this
                participants.add(student);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }

        return participants;
    }

    // Fetch teachers linked to a specific course
    public static List<User> getTeachersByCourse(String courseCode) {
        List<User> teachers = new java.util.ArrayList<>();
        try {
            String safeCode = java.net.URLEncoder.encode(courseCode, java.nio.charset.StandardCharsets.UTF_8.toString());
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/courses/" + safeCode + "/teachers"))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<java.util.Map<String, String>>>(){}.getType();
                List<java.util.Map<String, String>> dataList = gson.fromJson(response.body(), listType);

                for (java.util.Map<String, String> data : dataList) {
                    // Match the Teacher constructor: Teacher(id, name, email, password, designation)
                    com.syncron.models.Teacher t = new com.syncron.models.Teacher(
                            data.get("id"),
                            data.get("name"),
                            data.get("email"),
                            "",
                            "Lecturer"
                    );
                    teachers.add(t);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return teachers;
    }

    // quick lookup tool for the Profile page
    public static com.syncron.models.Course getCourseByCode(String code) {
        String query = "SELECT course_code, course_title, credits, type FROM courses WHERE course_code = ?";


        try (java.sql.Connection conn = connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, code);

            java.sql.ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new com.syncron.models.Course(
                        rs.getString("course_code"), rs.getString("course_title"),
                        rs.getString("credits"), rs.getString("type")
                );
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

}
