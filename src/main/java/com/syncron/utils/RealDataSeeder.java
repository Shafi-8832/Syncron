package com.syncron.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RealDataSeeder {

    public static void main(String[] args) {
        System.out.println("🚀 Starting Kernel Data Injection...");

        try (Connection conn = DatabaseHandler.connect()) {

            // 1. Create Tables (Added the new teacher_courses junction table!)
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, name TEXT, email TEXT, password TEXT, role TEXT, status TEXT, section TEXT, subsection TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS courses (course_code TEXT PRIMARY KEY, course_title TEXT, credits TEXT, type TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS teacher_courses (teacher_id TEXT, course_code TEXT)");

            // Clear old data for a clean slate
            stmt.execute("DELETE FROM users");
            stmt.execute("DELETE FROM courses");
            stmt.execute("DELETE FROM teacher_courses");

            // ---------------------------------------------------------
            // 2. INJECT STUDENTS
            // ---------------------------------------------------------
            System.out.println("📚 Reading buet_cse_7digit_rolls.txt...");
            List<String> studentLines = Files.readAllLines(Paths.get("buet_cse_7digit_rolls.txt"));

            String userSql = "INSERT INTO users (id, name, email, password, role, status, section, subsection) VALUES (?, ?, ?, ?, 'STUDENT', 'APPROVED', ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(userSql);

            int studentCount = 0;
            for (String line : studentLines) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(" ", 2);
                String id = parts[0].trim();
                String name = parts.length > 1 ? parts[1].trim() : "Unknown";

                int rollNumber = Integer.parseInt(id.substring(4));
                String section = (rollNumber <= 60) ? "A" : (rollNumber <= 120) ? "B" : "C";
                int rem = rollNumber % 60;
                if (rem == 0) rem = 60;
                String subSection = section + (rem <= 30 ? "1" : "2");

                pstmt.setString(1, id);
                pstmt.setString(2, name);
                pstmt.setString(3, id + "@buet.ac.bd");
                pstmt.setString(4, "buet123");
                pstmt.setString(5, section);
                pstmt.setString(6, subSection);
                pstmt.executeUpdate();
                studentCount++;
            }
            System.out.println("✅ Successfully injected " + studentCount + " Students!");

            // ---------------------------------------------------------
            // 3. INJECT EXACT COURSES
            // ---------------------------------------------------------
            String courseSql = "INSERT INTO courses (course_code, course_title, credits, type) VALUES (?, ?, ?, ?)";
            PreparedStatement courseStmt = conn.prepareStatement(courseSql);

            String[] knownCourseCodes = {"CSE 105", "CSE 106", "CSE 107", "CSE 108", "MATH 143", "CHEM 113", "ME 165", "ME 174"};
            String[][] courses = {
                    {knownCourseCodes[0], "Data Structures and Algorithms I", "3.0", "Theory"},
                    {knownCourseCodes[1], "Data Structures and Algorithms I Sessional", "1.5", "Sessional"},
                    {knownCourseCodes[2], "Object Oriented Programming Language", "3.0", "Theory"},
                    {knownCourseCodes[3], "Object Oriented Programming Language Sessional", "1.5", "Sessional"},
                    {knownCourseCodes[4], "Linear Algebra", "3.0", "Theory"},
                    {knownCourseCodes[5], "Chemistry", "3.0", "Theory"},
                    {knownCourseCodes[6], "Basic Mechanical Engineering", "3.0", "Theory"},
                    {knownCourseCodes[7], "Mechanical Engineering Drawing & AutoCAD", "1.5", "Sessional"}
            };

            for (String[] c : courses) {
                courseStmt.setString(1, c[0]);
                courseStmt.setString(2, c[1]);
                courseStmt.setString(3, c[2]);
                courseStmt.setString(4, c[3]);
                courseStmt.executeUpdate();
            }
            System.out.println("✅ Successfully injected 8 Real Courses!");

            // ---------------------------------------------------------
            // 4. THE SMART TEACHER PARSER (Now Links to Courses!)
            // ---------------------------------------------------------
            System.out.println("🎓 Parsing Teachers.txt and linking to courses...");
            List<String> teacherLines = Files.readAllLines(Paths.get("Teachers.txt"));

            String teacherSql = "INSERT INTO users (id, name, email, password, role, status) VALUES (?, ?, ?, 'buet123', 'TEACHER', 'APPROVED')";
            PreparedStatement teacherStmt = conn.prepareStatement(teacherSql);

            String assignSql = "INSERT INTO teacher_courses (teacher_id, course_code) VALUES (?, ?)";
            PreparedStatement assignStmt = conn.prepareStatement(assignSql);

            Map<String, String> existingTeachers = new HashMap<>(); // Prevents duplicate teacher accounts
            int teacherIdCounter = 101;
            String currentCourseContext = null;
            int assignmentCount = 0;

            for (String line : teacherLines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Step A: Detect which course we are currently reading
                if (line.startsWith("July 2025")) {
                    currentCourseContext = null;
                    for (String validCode : knownCourseCodes) {
                        String compactCode = validCode.replace(" ", ""); // "CSE105"
                        if (line.contains(validCode) || line.contains(compactCode)) {
                            currentCourseContext = validCode;
                            break;
                        }
                    }
                }
                // Step B: Detect a teacher and link them to the current course
                else if (line.startsWith("Teacher:") && currentCourseContext != null) {
                    String teacherName = line.substring(line.indexOf(":") + 1).trim();
                    String teacherId;

                    // If we haven't seen this teacher yet, create their account in 'users'
                    if (!existingTeachers.containsKey(teacherName)) {
                        teacherId = "T" + teacherIdCounter++;
                        existingTeachers.put(teacherName, teacherId);

                        String cleanEmail = teacherName.toLowerCase()
                                .replace("dr.", "").replace("md.", "").trim().replace(" ", ".") + "@cse.buet.ac.bd";

                        teacherStmt.setString(1, teacherId);
                        teacherStmt.setString(2, teacherName);
                        teacherStmt.setString(3, cleanEmail);
                        teacherStmt.executeUpdate();
                    } else {
                        // We already created their account earlier, just grab their ID!
                        teacherId = existingTeachers.get(teacherName);
                    }

                    // Link the teacher's ID to the current course code in 'teacher_courses'
                    assignStmt.setString(1, teacherId);
                    assignStmt.setString(2, currentCourseContext);
                    assignStmt.executeUpdate();
                    assignmentCount++;
                }
            }

            System.out.println("✅ Successfully injected " + existingTeachers.size() + " Unique Teachers!");
            System.out.println("🔗 Successfully created " + assignmentCount + " Teacher-to-Course Links!");
            System.out.println("🎉 INJECTION COMPLETE! Everything is perfectly mapped.");

        } catch (Exception e) {
            System.err.println("❌ ERROR during injection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}