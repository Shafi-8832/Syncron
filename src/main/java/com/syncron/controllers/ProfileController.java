package com.syncron.controllers;

import com.syncron.models.User;
import com.syncron.utils.ServerConfig;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class ProfileController {

    public static User viewingUser = null;
    private User loadedUser;
    private String currentPhotoPath = "";

    @FXML private VBox passwordFormBox, studentInfoContainer, studentHistoryBox, teacherAssignedBox;
    @FXML private HBox teacherInfoContainer;
    @FXML private Label nameLabel, roleLabel, sectionLabel, teacherIdLabel, passwordErrorLabel;
    @FXML private Label studentIdLabel;

    // Teacher Editable Fields
    @FXML private TextField bioField, contactField, githubField, linkedinField, fbField, roomField;
    @FXML private TextField teacherEmailLabel, studentEmailLabel;
    // Student Editable Fields
    @FXML private TextField studentBioField, studentGithubField, studentLinkedinField;

    // start change — Nickname field for both roles
    @FXML private TextField nicknameField;
    @FXML private TextField studentNicknameField;
    // end change

    @FXML private PasswordField currentPasswordField, newPasswordField, confirmPasswordField;
    @FXML private Circle profilePhotoCircle;

    // start change — Save feedback
    @FXML private Label saveStatusLabel;
    // end change

    @FXML
    public void initialize() {
        loadedUser = viewingUser != null ? viewingUser : SessionManager.getCurrentUser();
        viewingUser = null;

        hidePasswordForm();
        loadUserData(loadedUser);
    }

    private void loadUserData(User user) {
        if (user == null) return;
        nameLabel.setText(user.getName());
        roleLabel.setText(user.getRole() != null ? user.getRole().toUpperCase() : "UNKNOWN");

        if ("TEACHER".equalsIgnoreCase(user.getRole())) {
            teacherInfoContainer.setVisible(true); teacherInfoContainer.setManaged(true);
            teacherAssignedBox.setVisible(true); teacherAssignedBox.setManaged(true);
            studentInfoContainer.setVisible(false); studentInfoContainer.setManaged(false);
            studentHistoryBox.setVisible(false); studentHistoryBox.setManaged(false);

            teacherIdLabel.setText(user.getId());
            teacherEmailLabel.setText(user.getEmail());
            sectionLabel.setText(user.getId());
        } else {
            studentInfoContainer.setVisible(true); studentInfoContainer.setManaged(true);
            studentHistoryBox.setVisible(true); studentHistoryBox.setManaged(true);
            teacherInfoContainer.setVisible(false); teacherInfoContainer.setManaged(false);
            teacherAssignedBox.setVisible(false); teacherAssignedBox.setManaged(false);

            studentIdLabel.setText(user.getId());
            studentEmailLabel.setText(user.getEmail());
            sectionLabel.setText("Student");
        }

        // Fetch custom data from Cloud
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/users/" + user.getId() + "/profile")).GET().build();
            java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                Map<String, String> data = gson.fromJson(res.body(), new com.google.gson.reflect.TypeToken<Map<String, String>>(){}.getType());

                // Populate Teacher Fields
                if (bioField != null) bioField.setText(data.getOrDefault("bio", ""));
                if (contactField != null) contactField.setText(data.getOrDefault("contact_no", ""));
                if (githubField != null) githubField.setText(data.getOrDefault("github", ""));
                if (linkedinField != null) linkedinField.setText(data.getOrDefault("linkedin", ""));
                if (fbField != null) fbField.setText(data.getOrDefault("fb_link", ""));
                if (roomField != null) roomField.setText(data.getOrDefault("room_no", ""));

                // Populate Student Fields
                if (studentBioField != null) studentBioField.setText(data.getOrDefault("bio", ""));
                if (studentGithubField != null) studentGithubField.setText(data.getOrDefault("github", ""));
                if (studentLinkedinField != null) studentLinkedinField.setText(data.getOrDefault("linkedin", ""));

                // start change — Nickname
                String nickname = data.getOrDefault("nickname", "");
                if (nicknameField != null) nicknameField.setText(nickname);
                if (studentNicknameField != null) studentNicknameField.setText(nickname);
                // end change

                currentPhotoPath = data.getOrDefault("photo_path", "");
                if (!currentPhotoPath.isEmpty()) {
                    File imgFile = new File(currentPhotoPath);
                    if (imgFile.exists()) profilePhotoCircle.setFill(new ImagePattern(new Image(imgFile.toURI().toString())));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        loadAssignedCourses();
    }

    private void loadAssignedCourses() {
        try {
            String role = loadedUser.getRole();
            String name = loadedUser.getName().replace(" ", "%20");
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/dashboard/courses?role=" + role + "&name=" + name))
                    .GET().build();
            java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType();
                java.util.List<java.util.Map<String, String>> courses = gson.fromJson(res.body(), listType);

                VBox targetBox;
                if ("TEACHER".equalsIgnoreCase(role)) {
                    targetBox = (VBox) teacherAssignedBox.getChildren().get(1);
                } else {
                    targetBox = (VBox) studentHistoryBox.getChildren().get(1);
                }

                targetBox.getChildren().clear();
                if (courses.isEmpty()) {
                    Label empty = new Label("No courses " + ("TEACHER".equalsIgnoreCase(role) ? "assigned." : "enrolled."));
                    empty.setStyle("-fx-text-fill: #7F8C8D; -fx-font-style: italic;");
                    targetBox.getChildren().add(empty);
                } else {
                    for (java.util.Map<String, String> c : courses) {
                        Label lbl = new Label("• " + c.get("courseCode") + " - " + c.get("courseTitle"));
                        lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2980B9; -fx-cursor: hand;");
                        lbl.setOnMouseEntered(e -> lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3498DB; -fx-cursor: hand; -fx-underline: true;"));
                        lbl.setOnMouseExited(e -> lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2980B9; -fx-cursor: hand;"));

                        lbl.setOnMouseClicked(e -> {
                            try {
                                String courseCode = c.get("courseCode");
                                String courseTitle = c.get("courseTitle");
                                boolean isSessional = (courseTitle != null && courseTitle.toLowerCase().contains("sessional")) || (courseCode != null && courseCode.matches(".*[02468]$"));

                                SessionManager.setCurrentCourseCode(courseCode);
                                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/main_layout.fxml"));
                                javafx.scene.Parent root = loader.load();
                                MainController controller = loader.getController();
                                controller.setCourseContext(courseCode, courseTitle, isSessional ? "sessional" : "theory", isSessional ? "1.5" : "3.0");
                                javafx.stage.Stage stage = (javafx.stage.Stage) nameLabel.getScene().getWindow();
                                stage.getScene().setRoot(root);
                            } catch (Exception ex) { ex.printStackTrace(); }
                        });
                        targetBox.getChildren().add(lbl);
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // start change — Explicit Save Changes button handler
    @FXML
    private void handleSaveChanges() {
        saveProfileToCloud();

        // Show success feedback
        if (saveStatusLabel != null) {
            saveStatusLabel.setText("✓ Changes saved successfully!");
            saveStatusLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold; -fx-font-size: 13px;");
            saveStatusLabel.setVisible(true);
            saveStatusLabel.setManaged(true);

            // Fade out after 3 seconds
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> {
                    saveStatusLabel.setVisible(false);
                    saveStatusLabel.setManaged(false);
                });
            }).start();
        }
    }
    // end change

    private void saveProfileToCloud() {
        try {
            boolean isStudent = "STUDENT".equalsIgnoreCase(loadedUser.getRole());

            String bio = isStudent ? (studentBioField != null ? studentBioField.getText() : "") : (bioField != null ? bioField.getText() : "");
            String contact = isStudent ? "" : (contactField != null ? contactField.getText() : "");
            String github = isStudent ? (studentGithubField != null ? studentGithubField.getText() : "") : (githubField != null ? githubField.getText() : "");
            String linkedin = isStudent ? (studentLinkedinField != null ? studentLinkedinField.getText() : "") : (linkedinField != null ? linkedinField.getText() : "");
            String fb = isStudent ? "" : (fbField != null ? fbField.getText() : "");
            String room = isStudent ? "" : (roomField != null ? roomField.getText() : "");

            // start change — include nickname
            String nickname = isStudent ? (studentNicknameField != null ? studentNicknameField.getText() : "") : (nicknameField != null ? nicknameField.getText() : "");
            // end change

            String json = String.format("{\"bio\":\"%s\", \"contact_no\":\"%s\", \"github\":\"%s\", \"linkedin\":\"%s\", \"fb_link\":\"%s\", \"room_no\":\"%s\", \"photo_path\":\"%s\", \"nickname\":\"%s\"}",
                    bio.replace("\"", "\\\"").replace("\n", " "), contact, github, linkedin, fb, room,
                    currentPhotoPath.replace("\\", "\\\\"), nickname.replace("\"", "\\\""));

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/users/" + loadedUser.getId() + "/profile"))
                    .header("Content-Type", "application/json")
                    .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(json)).build();
            client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleUploadPhoto() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(nameLabel.getScene().getWindow());

        if (file != null) {
            try {
                File dir = new File("uploads/profiles");
                if (!dir.exists()) dir.mkdirs();
                File dest = new File(dir, loadedUser.getId() + "_" + file.getName());
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                currentPhotoPath = dest.getPath();
                profilePhotoCircle.setFill(new ImagePattern(new Image(dest.toURI().toString())));
                saveProfileToCloud();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void handleRemovePhoto() {
        currentPhotoPath = "";
        profilePhotoCircle.setFill(javafx.scene.paint.Color.web("#EAECEE"));
        saveProfileToCloud();
    }

    @FXML
    private void handleChangePassword() {
        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (currentPass.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
            passwordErrorLabel.setText("All fields required"); return;
        }
        if (!newPass.equals(confirmPass)) {
            passwordErrorLabel.setText("Passwords do not match"); return;
        }

        try {
            String json = String.format("{\"currentPassword\":\"%s\", \"newPassword\":\"%s\"}", currentPass, newPass);
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/users/" + loadedUser.getId() + "/password"))
                    .header("Content-Type", "application/json")
                    .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(json)).build();

            java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                hidePasswordForm();
                new Alert(Alert.AlertType.INFORMATION, "Password changed securely in the Cloud.").show();
            } else {
                passwordErrorLabel.setText("Current password incorrect.");
            }
        } catch (Exception e) { passwordErrorLabel.setText("Network Error"); }
    }

    @FXML private void handleShowPasswordForm() { passwordFormBox.setVisible(true); passwordFormBox.setManaged(true); }
    @FXML private void handleCancelPasswordChange() { hidePasswordForm(); }
    private void hidePasswordForm() { passwordFormBox.setVisible(false); passwordFormBox.setManaged(false); currentPasswordField.clear(); newPasswordField.clear(); confirmPasswordField.clear(); passwordErrorLabel.setText(""); }

    @FXML
    private void goBackToDashboard() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/home.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) nameLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        SessionManager.setCurrentUser(null);
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/login.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) nameLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}