package com.syncron.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Map;

public class AdminController {

    @FXML private VBox dashboardView;
    @FXML private VBox pendingView;
    @FXML private Label deptHeaderLabel;

    @FXML private VBox coursesListContainer;
    @FXML private VBox teachersListContainer;
    @FXML private VBox studentsListContainer;

    @FXML private VBox pendingUsersContainer;
    @FXML private TextArea terminalLog;

    @FXML
    public void initialize() {
        log("KERNEL v1.0.8 DARK ADMIN INITIALIZED...");
        log("Secure API Connection Established.");

        // CSS FUELED HEADER GLOW LOGIC
        String normalStyle = "-fx-font-family: 'Georgia', serif; -fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #10B981; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(16, 185, 129, 0.2), 10, 0, 0, 0);";
        String glowStyle = "-fx-font-family: 'Georgia', serif; -fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #34D399; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(52, 211, 153, 0.8), 25, 0, 0, 0);";
        deptHeaderLabel.setStyle(normalStyle);
        deptHeaderLabel.setOnMouseEntered(e -> deptHeaderLabel.setStyle(glowStyle));
        deptHeaderLabel.setOnMouseExited(e -> deptHeaderLabel.setStyle(normalStyle));

        handleDashboard(); // Load default view
    }

    private void log(String message) {
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (terminalLog != null) {
            terminalLog.appendText("[" + time + "] " + message + "\n");
        }
    }

    // --- VIEW TOGGLERS ---
    @FXML
    private void handleDashboard() {
        dashboardView.setVisible(true);
        pendingView.setVisible(false);
        log("CMD: Refreshing System Dashboard Data...");
        loadDashboardStats();
    }

    @FXML
    private void handlePendingApprovals() {
        dashboardView.setVisible(false);
        pendingView.setVisible(true);
        log("CMD: Accessing Pending Approvals Queue...");
        loadPendingUsers();
    }

    @FXML private void handleManageUsers() { log("CMD: Manage Users... [Accessing external module]"); }
    @FXML private void handleManageCourses() { log("CMD: Manage Courses... [Accessing external module]"); }
    @FXML private void handleManageSemesters() { log("CMD: Manage Semesters... [Accessing external module]"); }

    // --- DATA LOADERS ---
    private void loadDashboardStats() {
        // 1. Fetch Courses
        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create("http://localhost:8080/api/courses")).GET().build();
                java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    List<Map<String, Object>> courses = gson.fromJson(res.body(), new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType());
                    Platform.runLater(() -> {
                        coursesListContainer.getChildren().clear();
                        for (Map<String, Object> c : courses) {
                            Label lbl = new Label("🔹 " + c.get("courseCode") + " - " + c.get("courseTitle"));
                            lbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 14px;");
                            coursesListContainer.getChildren().add(lbl);
                        }
                    });
                }
            } catch (Exception e) { log("ERR: Failed to load courses."); }
        }).start();

        // 2. Fetch Users (181 Students + Teachers)
        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create("http://localhost:8080/api/admin/all-users")).GET().build();
                java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    List<Map<String, String>> users = gson.fromJson(res.body(), new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType());
                    Platform.runLater(() -> {
                        teachersListContainer.getChildren().clear();
                        studentsListContainer.getChildren().clear();

                        for (Map<String, String> u : users) {
                            if ("TEACHER".equalsIgnoreCase(u.get("role"))) {
                                // 👉 Teachers get their Email shown
                                Label lbl = new Label("• " + u.get("name") + " (" + u.get("email") + ")");
                                lbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
                                teachersListContainer.getChildren().add(lbl);
                            }
                            else if ("STUDENT".equalsIgnoreCase(u.get("role"))) {
                                // 👉 Students keep their ID shown
                                Label lbl = new Label("• " + u.get("name") + " (" + u.get("id") + ")");
                                lbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
                                studentsListContainer.getChildren().add(lbl);
                            }
                        }

                        log("INFO: Loaded " + studentsListContainer.getChildren().size() + " Students and " + teachersListContainer.getChildren().size() + " Teachers.");
                    });
                }
            } catch (Exception e) { log("ERR: Failed to load users."); }
        }).start();
    }

    private void loadPendingUsers() {
        pendingUsersContainer.getChildren().clear();
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create("http://localhost:8080/api/admin/pending-users")).GET().build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                List<Map<String, String>> users = gson.fromJson(response.body(), new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType());

                if (users.isEmpty()) {
                    Label empty = new Label("No pending users. You're all caught up!");
                    empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
                    pendingUsersContainer.getChildren().add(empty);
                    return;
                }

                for (Map<String, String> user : users) {
                    HBox row = new HBox(15);
                    row.setStyle("-fx-background-color: #1E293B; -fx-padding: 20; -fx-border-radius: 8; -fx-border-color: #334155;");
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    VBox info = new VBox(5);
                    Label name = new Label(user.get("name") + " (" + user.get("id") + ")");
                    name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #F8FAFC;");
                    Label role = new Label(user.get("role") + " • " + user.get("email"));
                    role.setStyle("-fx-text-fill: #94A3B8;");
                    info.getChildren().addAll(name, role);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Button approveBtn = new Button("✔ Approve User");
                    approveBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");
                    approveBtn.setOnAction(e -> approveUser(user.get("id")));

                    row.getChildren().addAll(info, spacer, approveBtn);
                    pendingUsersContainer.getChildren().add(row);
                }
            }
        } catch (Exception e) { log("ERR: Failed to fetch pending users."); }
    }

    private void approveUser(String id) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/admin/approve/" + id))
                    .PUT(java.net.http.HttpRequest.BodyPublishers.noBody()).build();
            client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            log("SUCCESS: User " + id + " has been verified.");
            loadPendingUsers();
        } catch (Exception e) { log("ERR: Failed to approve user " + id); }
    }

    @FXML
    private void handleLogout() {
        log("Disconnecting secure session...");
        SessionManager.setCurrentUser(null);
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/login.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) pendingUsersContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}