package com.syncron.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileHandler {

    // This creates a folder named "syncron_uploads" in your main project directory
    private static final String UPLOAD_DIR = "syncron_uploads";

    public static String saveFile(File sourceFile, String studentId, String moduleTitle) {
        try {
            // 1. Check if the folder exists, if not, create it
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdir();
            }

            // 2. Clean up the title to remove spaces (e.g. "Final Project" -> "Final_Project")
            String cleanTitle = moduleTitle.replaceAll("\\s+", "_");

            // 3. Create a safe, unique file name: "101_Final_Project_mycode.zip"
            String newFileName = studentId + "_" + cleanTitle + "_" + sourceFile.getName();
            Path destination = new File(dir, newFileName).toPath();

            // 4. Copy the file over
            Files.copy(sourceFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File saved successfully at: " + destination.toString());
            return destination.toString(); // We will save this path in the database later

        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
            return null;
        }
    }
}