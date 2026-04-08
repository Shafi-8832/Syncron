package com.syncron.controllers;

import com.syncron.utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.io.File;
import java.util.List;
import java.util.Map;

public class PublicProfileController {

    @FXML private Circle profilePhoto;
    @FXML private Label nameLabel;
    @FXML private Label roleBadge;
    @FXML private VBox infoContainer;
    @FXML private Label coursesSectionTitle;
    @FXML private VBox coursesContainer;

    private String targetUserId;
    private String targetUserRole;
    private String targetUserName;

    @FXML
    public void initialize() {
        targetUserId = SessionManager.getViewProfileId();
        if (targetUserId == null || targetUserId.isEmpty()) {
            nameLabel.setText("User Not Found");
            return;
        }

        NavigationManager.updateGlobalBreadcrumb("Participants / Profile");
        loadPublicProfile();
    }

    private void loadPublicProfile() {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            // Fetch basic user info + profile extras
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/users/" + targetUserId + "/profile"))
                    .GET().build();
            java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            // Also fetch user identity (name, role, email) from the admin endpoint
            java.net.http.HttpRequest identityReq = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/admin/all-users"))
                    .GET().build();
            java.net.http.HttpResponse<String> identityRes = client.send(identityReq, java.net.http.HttpResponse.BodyHandlers.ofString());

            // Find our target user in the users list
            String userName = "Unknown";
            String userRole = "STUDENT";
            String userEmail = "";
            String userSection = "";
            String userSubsection = "";

            if (identityRes.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType();
                List<Map<String, String>> allUsers = gson.fromJson(identityRes.body(), listType);

                for (Map<String, String> u : allUsers) {
                    if (targetUserId.equals(u.get("id"))) {
                        userName = u.get("name") != null ? u.get("name") : "Unknown";
                        userRole = u.get("role") != null ? u.get("role") : "STUDENT";
                        userEmail = u.get("email") != null ? u.get("email") : "";
                        userSection = u.get("section") != null ? u.get("section") : "";
                        userSubsection = u.get("subsection") != null ? u.get("subsection") : "";
                        break;
                    }
                }
            }

            targetUserRole = userRole;
            targetUserName = userName;

            // Set name and role badge
            nameLabel.setText(userName);

            if ("TEACHER".equalsIgnoreCase(userRole)) {
                roleBadge.setText("TEACHER");
                roleBadge.setStyle("-fx-background-color: #D35400; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 14; -fx-background-radius: 14; -fx-font-size: 12px;");
                coursesSectionTitle.setText("Courses Assigned");
            } else {
                roleBadge.setText("STUDENT");
                roleBadge.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 14; -fx-background-radius: 14; -fx-font-size: 12px;");
                coursesSectionTitle.setText("Courses Enrolled");
            }

            // Build info rows
            infoContainer.getChildren().clear();

            if ("TEACHER".equalsIgnoreCase(userRole)) {
                // Teacher public profile: email, contact, room
                addInfoRow("Email", userEmail);

                // Load profile extras (contact_no, room_no)
                if (res.statusCode() == 200) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    Map<String, String> profile = gson.fromJson(res.body(),
                            new com.google.gson.reflect.TypeToken<Map<String, String>>(){}.getType());

                    String contact = profile.getOrDefault("contact_no", "");
                    String room = profile.getOrDefault("room_no", "");
                    String photoPath = profile.getOrDefault("photo_path", "");

                    if (!contact.isEmpty()) addInfoRow("Contact", contact);
                    if (!room.isEmpty()) addInfoRow("Room No", room);

                    // Load photo
                    if (!photoPath.isEmpty()) {
                        File imgFile = new File(photoPath);
                        if (imgFile.exists()) {
                            profilePhoto.setFill(new ImagePattern(new Image(imgFile.toURI().toString())));
                        }
                    }
                }
            } else {
                // Student public profile: ID, email, section, subsection
                addInfoRow("Student ID", targetUserId);
                if (!userEmail.isEmpty()) addInfoRow("Email", userEmail);

                // Calculate section/subsection from Student model logic
                if (!userSection.isEmpty()) addInfoRow("Section", userSection);
                if (!userSubsection.isEmpty()) {
                    addInfoRow("Subsection", userSubsection);
                } else {
                    // Calculate subsection from roll number (matching Student.java logic)
                    try {
                        String idStr = targetUserId;
                        if (idStr.length() >= 3) {
                            int roll = Integer.parseInt(idStr.substring(idStr.length() - 3));
                            String sub = "--";
                            if (roll >= 1 && roll <= 30) sub = "A1";
                            else if (roll >= 31 && roll <= 60) sub = "A2";
                            else if (roll >= 61 && roll <= 90) sub = "B1";
                            else if (roll >= 91 && roll <= 120) sub = "B2";
                            else if (roll >= 121 && roll <= 150) sub = "C1";
                            else if (roll >= 151 && roll <= 181) sub = "C2";
                            addInfoRow("Subsection", sub);
                        }
                    } catch (NumberFormatException ignored) {}
                }

                // Load photo
                if (res.statusCode() == 200) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    Map<String, String> profile = gson.fromJson(res.body(),
                            new com.google.gson.reflect.TypeToken<Map<String, String>>(){}.getType());
                    String photoPath = profile.getOrDefault("photo_path", "");
                    if (!photoPath.isEmpty()) {
                        File imgFile = new File(photoPath);
                        if (imgFile.exists()) {
                            profilePhoto.setFill(new ImagePattern(new Image(imgFile.toURI().toString())));
                        }
                    }
                }
            }

            // Load courses
            loadCourses();

        } catch (Exception e) {
            e.printStackTrace();
            nameLabel.setText("Error loading profile");
        }
    }

    private void addInfoRow(String label, String value) {
        if (value == null || value.isEmpty() || value.equals("null")) return;

        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 0, 6, 0));
        row.setStyle("-fx-border-color: #F5F5F5; -fx-border-width: 0 0 1 0;");

        Label keyLabel = new Label(label);
        keyLabel.setMinWidth(120);
        keyLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: #2C3E50; -fx-font-size: 14px;");
        valueLabel.setWrapText(true);

        row.getChildren().addAll(keyLabel, valueLabel);
        infoContainer.getChildren().add(row);
    }

    private void loadCourses() {
        coursesContainer.getChildren().clear();

        try {
            String role = targetUserRole;
            String name = targetUserName.replace(" ", "%20");

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/dashboard/courses?role=" + role + "&name=" + name))
                    .GET().build();
            java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType();
                List<Map<String, String>> courses = gson.fromJson(res.body(), listType);

                if (courses.isEmpty()) {
                    Label empty = new Label("No courses found.");
                    empty.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
                    coursesContainer.getChildren().add(empty);
                    return;
                }

                for (Map<String, String> c : courses) {
                    String code = c.get("courseCode");
                    String title = c.get("courseTitle");

                    HBox courseRow = new HBox(12);
                    courseRow.setAlignment(Pos.CENTER_LEFT);
                    courseRow.setPadding(new Insets(10, 15, 10, 15));
                    courseRow.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;");
                    courseRow.setOnMouseEntered(e -> courseRow.setStyle("-fx-background-color: #F0F7FF; -fx-background-radius: 8; -fx-cursor: hand;"));
                    courseRow.setOnMouseExited(e -> courseRow.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;"));

                    Label codeLabel = new Label(code != null ? code : "");
                    codeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2980B9;");

                    Label titleLabel = new Label(title != null ? " — " + title : "");
                    titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7F8C8D;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label arrow = new Label("→");
                    arrow.setStyle("-fx-text-fill: #BDC3C7; -fx-font-size: 16px;");

                    courseRow.getChildren().addAll(codeLabel, titleLabel, spacer, arrow);

                    // Click → navigate to the course with correct breadcrumb & sidebar on "Common"
                    courseRow.setOnMouseClicked(e -> navigateToCourse(code, title));

                    coursesContainer.getChildren().add(courseRow);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Label err = new Label("Could not load courses.");
            err.setStyle("-fx-text-fill: #E74C3C;");
            coursesContainer.getChildren().add(err);
        }
    }

    private void navigateToCourse(String courseCode, String courseTitle) {
        if (courseCode == null) return;

        try {
            // Determine course type from the title
            boolean isSessional = (courseTitle != null && courseTitle.toLowerCase().contains("sessional"))
                    || courseCode.matches(".*[02468]$");
            String courseType = isSessional ? "sessional" : "theory";
            String credits = isSessional ? "1.5" : "3.0";

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/syncron/views/main_layout.fxml"));
            javafx.scene.Parent root = loader.load();

            SessionManager.setCurrentCourseCode(courseCode);

            MainController controller = loader.getController();
            controller.setCourseContext(courseCode, courseTitle != null ? courseTitle : "Course", courseType, credits);
            // setCourseContext already navigates to common.fxml and sets sidebar to "Common"

            javafx.stage.Stage stage = (javafx.stage.Stage) nameLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        NavigationManager.switchScreen("participants.fxml");
        NavigationManager.updateGlobalBreadcrumb("Participants");
    }
}