package com.syncron.controllers;

import com.syncron.utils.ServerConfig;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminController {

    @FXML private VBox dashboardView;
    @FXML private VBox pendingView;
    // start change — new assignments view
    @FXML private VBox assignmentsView;
    // end change
    @FXML private Label deptHeaderLabel;

    @FXML private VBox coursesListContainer;
    @FXML private VBox teachersListContainer;
    @FXML private VBox studentsListContainer;

    @FXML private VBox pendingUsersContainer;
    @FXML private TextArea terminalLog;

    // start change — assignment management state
    @FXML private ComboBox<String> courseSelector;
    @FXML private VBox assignedTeachersContainer;
    @FXML private ComboBox<String> teacherSelector;

    private List<Map<String, String>> allCourses = new ArrayList<>();
    private List<Map<String, String>> allTeachers = new ArrayList<>();
    // end change

    @FXML
    public void initialize() {
        log("KERNEL v1.0.8 DARK ADMIN INITIALIZED...");
        log("Secure API Connection Established.");

        String normalStyle = "-fx-font-family: 'Georgia', serif; -fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #10B981; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(16, 185, 129, 0.2), 10, 0, 0, 0);";
        String glowStyle = "-fx-font-family: 'Georgia', serif; -fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #34D399; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(52, 211, 153, 0.8), 25, 0, 0, 0);";
        deptHeaderLabel.setStyle(normalStyle);
        deptHeaderLabel.setOnMouseEntered(e -> deptHeaderLabel.setStyle(glowStyle));
        deptHeaderLabel.setOnMouseExited(e -> deptHeaderLabel.setStyle(normalStyle));

        handleDashboard();
    }

    private void log(String message) {
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (terminalLog != null) {
            terminalLog.appendText("[" + time + "] " + message + "\n");
        }
    }

    // =========================================================================
    // VIEW TOGGLERS
    // =========================================================================

    @FXML
    private void handleDashboard() {
        dashboardView.setVisible(true); dashboardView.setManaged(true);
        pendingView.setVisible(false); pendingView.setManaged(false);
        if (assignmentsView != null) { assignmentsView.setVisible(false); assignmentsView.setManaged(false); }
        log("CMD: Refreshing System Dashboard Data...");
        loadDashboardStats();
    }

    @FXML
    private void handlePendingApprovals() {
        dashboardView.setVisible(false); dashboardView.setManaged(false);
        pendingView.setVisible(true); pendingView.setManaged(true);
        if (assignmentsView != null) { assignmentsView.setVisible(false); assignmentsView.setManaged(false); }
        log("CMD: Accessing Pending Approvals Queue...");
        loadPendingUsers();
    }

    // start change — new view toggler for assignments
    @FXML
    private void handleManageCourses() {
        dashboardView.setVisible(false); dashboardView.setManaged(false);
        pendingView.setVisible(false); pendingView.setManaged(false);
        if (assignmentsView != null) { assignmentsView.setVisible(true); assignmentsView.setManaged(true); }
        log("CMD: Opening Course Assignment Manager...");
        loadAssignmentManager();
    }
    // end change

    @FXML private void handleManageUsers() { log("CMD: Manage Users... [Feature coming soon]"); }
    @FXML private void handleManageSemesters() { log("CMD: Manage Semesters... [Feature coming soon]"); }

    // =========================================================================
    // DASHBOARD STATS (existing)
    // =========================================================================

    private void loadDashboardStats() {
        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/courses")).GET().build();
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

        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/admin/all-users")).GET().build();
                java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    List<Map<String, String>> users = gson.fromJson(res.body(), new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType());
                    Platform.runLater(() -> {
                        teachersListContainer.getChildren().clear();
                        studentsListContainer.getChildren().clear();
                        for (Map<String, String> u : users) {
                            if ("TEACHER".equalsIgnoreCase(u.get("role"))) {
                                Label lbl = new Label("• " + u.get("name") + " (" + u.get("email") + ")");
                                lbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
                                teachersListContainer.getChildren().add(lbl);
                            } else if ("STUDENT".equalsIgnoreCase(u.get("role"))) {
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

    // =========================================================================
    // PENDING USERS (existing)
    // =========================================================================

    private void loadPendingUsers() {
        pendingUsersContainer.getChildren().clear();
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/admin/pending-users")).GET().build();
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
                    row.setAlignment(Pos.CENTER_LEFT);

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
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/admin/approve/" + id))
                    .PUT(java.net.http.HttpRequest.BodyPublishers.noBody()).build();
            client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            log("SUCCESS: User " + id + " has been verified.");
            loadPendingUsers();
        } catch (Exception e) { log("ERR: Failed to approve user " + id); }
    }

    // =========================================================================
    // start change — COURSE ASSIGNMENT MANAGER (brand new)
    // =========================================================================

    private void loadAssignmentManager() {
        if (courseSelector == null || teacherSelector == null || assignedTeachersContainer == null) {
            log("ERR: Assignment UI elements not found. Check admin_dashboard.fxml.");
            return;
        }

        // 1. Fetch all courses
        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                com.google.gson.Gson gson = new com.google.gson.Gson();

                // Courses
                java.net.http.HttpRequest cReq = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/courses")).GET().build();
                java.net.http.HttpResponse<String> cRes = client.send(cReq, java.net.http.HttpResponse.BodyHandlers.ofString());

                // Teachers
                java.net.http.HttpRequest tReq = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/admin/all-users")).GET().build();
                java.net.http.HttpResponse<String> tRes = client.send(tReq, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (cRes.statusCode() == 200 && tRes.statusCode() == 200) {
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType();

                    java.lang.reflect.Type courseListType = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> rawCourses = gson.fromJson(cRes.body(), courseListType);

                    List<Map<String, String>> courses = new ArrayList<>();
                    for (Map<String, Object> m : rawCourses) {
                        Map<String, String> flat = new HashMap<>();
                        flat.put("courseCode", String.valueOf(m.get("courseCode")));
                        flat.put("courseTitle", String.valueOf(m.get("courseTitle")));
                        courses.add(flat);
                    }

                    List<Map<String, String>> users = gson.fromJson(tRes.body(), listType);
                    List<Map<String, String>> teachers = new ArrayList<>();
                    for (Map<String, String> u : users) {
                        if ("TEACHER".equalsIgnoreCase(u.get("role"))) teachers.add(u);
                    }

                    allCourses = courses;
                    allTeachers = teachers;

                    Platform.runLater(() -> {
                        courseSelector.getItems().clear();
                        for (Map<String, String> c : courses) {
                            courseSelector.getItems().add(c.get("courseCode") + " — " + c.get("courseTitle"));
                        }

                        teacherSelector.getItems().clear();
                        for (Map<String, String> t : teachers) {
                            teacherSelector.getItems().add(t.get("name") + " (" + t.get("id") + ")");
                        }

                        // Auto-select first course
                        if (!courseSelector.getItems().isEmpty()) {
                            courseSelector.setValue(courseSelector.getItems().get(0));
                            loadAssignedTeachersForCourse();
                        }

                        courseSelector.setOnAction(e -> loadAssignedTeachersForCourse());

                        log("INFO: Assignment Manager loaded. " + courses.size() + " courses, " + teachers.size() + " teachers.");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                log("ERR: Could not load assignment data.");
            }
        }).start();
    }

    private String getSelectedCourseCode() {
        String selected = courseSelector.getValue();
        if (selected == null) return null;
        // Extract course code from "CSE 105 — Data Structures..."
        return selected.split(" — ")[0].trim();
    }

    private void loadAssignedTeachersForCourse() {
        assignedTeachersContainer.getChildren().clear();
        String courseCode = getSelectedCourseCode();
        if (courseCode == null) return;

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/courses/" + courseCode.replace(" ", "%20") + "/teachers"))
                    .GET().build();
            java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType();
                List<Map<String, String>> teachers = gson.fromJson(res.body(), listType);

                if (teachers.isEmpty()) {
                    Label empty = new Label("No teachers assigned to this course yet.");
                    empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
                    assignedTeachersContainer.getChildren().add(empty);
                } else {
                    for (Map<String, String> t : teachers) {
                        HBox row = new HBox(15);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setStyle("-fx-background-color: #1E293B; -fx-padding: 12 20; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-color: #334155;");

                        Label nameLabel = new Label(t.get("name") + " (" + t.get("id") + ")");
                        nameLabel.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 14px; -fx-font-weight: bold;");

                        Label emailLabel = new Label(t.get("email"));
                        emailLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

                        VBox info = new VBox(3);
                        info.getChildren().addAll(nameLabel, emailLabel);

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        Button removeBtn = new Button("✕ Remove");
                        String rmBase = "-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: #EF4444; -fx-border-radius: 6; -fx-padding: 5 12; -fx-font-size: 12px;";
                        String rmHover = "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: #EF4444; -fx-border-radius: 6; -fx-padding: 5 12; -fx-font-size: 12px;";
                        removeBtn.setStyle(rmBase);
                        removeBtn.setOnMouseEntered(e -> removeBtn.setStyle(rmHover));
                        removeBtn.setOnMouseExited(e -> removeBtn.setStyle(rmBase));

                        final String teacherId = t.get("id");
                        final String teacherName = t.get("name");
                        removeBtn.setOnAction(e -> unassignTeacher(courseCode, teacherId, teacherName));

                        row.getChildren().addAll(info, spacer, removeBtn);
                        assignedTeachersContainer.getChildren().add(row);
                    }
                }

                log("INFO: " + teachers.size() + " teacher(s) assigned to " + courseCode);
            }
        } catch (Exception e) {
            log("ERR: Could not fetch assigned teachers.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAssignTeacher() {
        String courseCode = getSelectedCourseCode();
        String teacherSelection = teacherSelector.getValue();
        if (courseCode == null || teacherSelection == null) {
            log("WARN: Select both a course and a teacher first.");
            return;
        }

        // Extract teacher ID and name from "Name (ID)"
        String teacherId = teacherSelection.substring(teacherSelection.lastIndexOf("(") + 1, teacherSelection.lastIndexOf(")"));
        String teacherName = teacherSelection.substring(0, teacherSelection.lastIndexOf("(")).trim();

        try {
            String json = String.format("{\"courseCode\":\"%s\", \"teacherId\":\"%s\", \"teacherName\":\"%s\"}",
                    courseCode, teacherId, teacherName);

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/admin/assign-teacher"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();

            java.net.http.HttpResponse<String> res = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                log("SUCCESS: Assigned " + teacherName + " to " + courseCode);
                loadAssignedTeachersForCourse(); // Refresh
            } else if (res.statusCode() == 409) {
                log("WARN: " + teacherName + " is already assigned to " + courseCode);
            } else {
                log("ERR: Server returned " + res.statusCode());
            }
        } catch (Exception e) {
            log("ERR: Network error during assignment.");
            e.printStackTrace();
        }
    }

    private void unassignTeacher(String courseCode, String teacherId, String teacherName) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + teacherName + " from " + courseCode + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Removal");
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            try {
                String json = String.format("{\"courseCode\":\"%s\", \"teacherId\":\"%s\", \"teacherName\":\"%s\"}",
                        courseCode, teacherId, teacherName);

                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/admin/unassign-teacher"))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                        .build();

                java.net.http.HttpResponse<String> res = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    log("SUCCESS: Removed " + teacherName + " from " + courseCode);
                    loadAssignedTeachersForCourse();
                } else {
                    log("ERR: Server returned " + res.statusCode());
                }
            } catch (Exception e) {
                log("ERR: Network error during unassignment.");
                e.printStackTrace();
            }
        }
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