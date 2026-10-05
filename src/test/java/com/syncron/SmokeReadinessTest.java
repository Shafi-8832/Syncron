package com.syncron;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmokeReadinessTest {

    @Test
    void criticalViewsExist() {
        List<String> criticalViews = List.of(
                "login.fxml",
                "home.fxml",
                "main_layout.fxml",
                "announcements.fxml",
                "evaluation_details.fxml",
                "profile.fxml",
                "public_profile.fxml"
        );

        for (String view : criticalViews) {
            Path path = Paths.get("src/main/resources/com/syncron/views", view);
            assertTrue(Files.exists(path), "Missing critical view: " + view);
        }
    }

    @Test
    void noHardcodedLocalhostApiCallsInControllers() throws Exception {
        Path sourceRoot = Paths.get("src/main/java/com/syncron/controllers");
        assertTrue(Files.exists(sourceRoot), "Controllers source directory not found.");

        try (var stream = Files.walk(sourceRoot)) {
            List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                String text = Files.readString(file);
                assertFalse(text.contains("http://localhost:8080/api/"),
                        "Hardcoded localhost API URL found in " + file);
            }
        }
    }
}
