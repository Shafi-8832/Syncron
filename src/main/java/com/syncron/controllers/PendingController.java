package com.syncron.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class PendingController {

    @FXML private VBox coursesListContainer;
    @FXML private VBox teachersListContainer;
    @FXML private VBox studentsListContainer;
    @FXML private Label studentCountLabel;

    @FXML
    public void initialize() {
        loadPreviewData();
    }

    private void loadPreviewData() {
        // 1. Fetch Live Courses
        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/courses"))
                        .GET().build();
                java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    List<Map<String, Object>> courses = gson.fromJson(res.body(), new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType());

                    Platform.runLater(() -> {
                        coursesListContainer.getChildren().clear();
                        for (Map<String, Object> c : courses) {
                            Label lbl = new Label("🔹 " + c.get("courseCode") + " - " + c.get("courseTitle"));
                            lbl.setStyle("-fx-text-fill: #16A085; -fx-font-size: 13px; -fx-font-weight: bold;");
                            lbl.setWrapText(true);
                            coursesListContainer.getChildren().add(lbl);
                        }
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        // 2. Fetch Live Approved Users (Teachers & Students)
        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/admin/all-users"))
                        .GET().build();
                java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    List<Map<String, String>> users = gson.fromJson(res.body(), new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType());

                    Platform.runLater(() -> {
                        teachersListContainer.getChildren().clear();
                        studentsListContainer.getChildren().clear();

                        for (Map<String, String> u : users) {
                            Label lbl = new Label("• " + u.get("name") + " (" + u.get("id") + ")");
                            lbl.setWrapText(true);

                            if ("TEACHER".equalsIgnoreCase(u.get("role"))) {
                                lbl.setStyle("-fx-text-fill: #2980B9; -fx-font-size: 13px; -fx-font-weight: bold;");
                                teachersListContainer.getChildren().add(lbl);
                            } else if ("STUDENT".equalsIgnoreCase(u.get("role"))) {
                                lbl.setStyle("-fx-text-fill: #C0392B; -fx-font-size: 13px;");
                                studentsListContainer.getChildren().add(lbl);
                            }
                        }

                        // Update the title dynamically based on how many students are actually in the DB!
                        studentCountLabel.setText("🎓 " + studentsListContainer.getChildren().size() + " Enrolled Students");
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.setCurrentUser(null);
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/login.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}