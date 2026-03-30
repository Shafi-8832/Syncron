// ==========================================
// 🟢 START OF DatabaseUpgrade.java
// ==========================================
package com.syncron.utils;

import java.sql.Connection;
import java.sql.Statement;

public class EvaluationSeeder {
    public static void main(String[] args) {
        System.out.println("🚀 Starting Database Upgrade...");

        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement()) {

            // Table 1: Stores the actual Offline/Online details
            stmt.execute("CREATE TABLE IF NOT EXISTS evaluations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "course_code TEXT, title TEXT, type TEXT, " +
                    "target_sections TEXT, total_marks TEXT, " +
                    "deadline_date TEXT, deadline_time TEXT, " +
                    "description TEXT, creator_id TEXT)");

            // Table 2: Stores the files students drag-and-drop later
            stmt.execute("CREATE TABLE IF NOT EXISTS submissions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "evaluation_id INTEGER, student_id TEXT, " +
                    "file_path TEXT, submission_time TEXT, grade TEXT)");

            // Table 3: Powers the "Common" Tab feed
            stmt.execute("CREATE TABLE IF NOT EXISTS announcements (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "course_code TEXT, message TEXT, " +
                    "timestamp TEXT, creator_id TEXT)");

            System.out.println("✅ Success! Added evaluations, submissions, and announcements tables.");

        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
// ==========================================
// 🔴 END OF DatabaseUpgrade.java
// ==========================================