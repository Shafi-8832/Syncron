package com.syncron.controllers;

import com.syncron.utils.NavigationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateAnnouncementController {

    @FXML private Label authorNameLabel;
    @FXML private TextArea postBodyInput;
    @FXML private Button publishBtn;

    // 👉 THE @MENTION ENGINE VARIABLES
    private ContextMenu mentionMenu = new ContextMenu();
    private List<Map<String, String>> cachedAssessments = new ArrayList<>();

    @FXML
    public void initialize() {
        NavigationManager.updateGlobalBreadcrumb("Announcements / Create Post");

        if (SessionManager.getCurrentUser() != null) {
            authorNameLabel.setText(SessionManager.getCurrentUser().getName());
        }

        String baseStyle = "-fx-background-color: #2980B9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 14px;";
        String hoverStyle = baseStyle + " -fx-effect: dropshadow(three-pass-box, rgba(41, 128, 185, 0.5), 10, 0, 0, 0); -fx-background-color: #3498DB;";
        publishBtn.setStyle(baseStyle);
        publishBtn.setOnMouseEntered(e -> publishBtn.setStyle(hoverStyle));
        publishBtn.setOnMouseExited(e -> publishBtn.setStyle(baseStyle));

        // 1. Pre-fetch assessments silently in the background
        fetchAssessmentsForMentions();

        // 2. Activate the Keystroke Listener!
        setupMentionEngine();
    }

    private void fetchAssessmentsForMentions() {
        new Thread(() -> {
            try {
                String courseCode = SessionManager.getCurrentCourseCode();
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/evaluations/course/" + courseCode.replace(" ", "%20")))
                        .GET().build();

                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType();
                    cachedAssessments = gson.fromJson(response.body(), listType);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void setupMentionEngine() {
        // Style the floating popup menu
        mentionMenu.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #BDC3C7; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        postBodyInput.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && !newText.isEmpty()) {
                // If the user just typed an '@' character
                if (newText.charAt(newText.length() - 1) == '@') {
                    showMentionMenu();
                } else if (mentionMenu.isShowing()) {
                    mentionMenu.hide();
                }
            }
        });
    }

    private void showMentionMenu() {
        if (cachedAssessments.isEmpty()) return;

        mentionMenu.getItems().clear();

        // Build the dropdown items
        for (Map<String, String> eval : cachedAssessments) {
            MenuItem item = new MenuItem("📎 " + eval.get("title") + " (" + eval.get("type") + ")");
            item.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-text-fill: #2C3E50; -fx-padding: 5 15;");

            item.setOnAction(e -> {
                // The Smart Tag logic
                String smartTag = String.format("[ASSESSMENT:%s:%s]", eval.get("id"), eval.get("title"));

                // Replace the '@' with the full Smart Tag inline
                String currentText = postBodyInput.getText();
                postBodyInput.setText(currentText.substring(0, currentText.length() - 1) + smartTag + " ");

                // Move cursor to the end
                postBodyInput.positionCaret(postBodyInput.getText().length());
            });
            mentionMenu.getItems().add(item);
        }

        // Show the menu floating just below the text area
        Bounds bounds = postBodyInput.localToScreen(postBodyInput.getBoundsInLocal());
        if (bounds != null) {
            mentionMenu.show(postBodyInput, bounds.getMinX() + 10, bounds.getMaxY() - 30);
        }
    }

    @FXML
    private void handlePublish() {
        String message = postBodyInput.getText().trim();
        if (message.isEmpty()) return;

        try {
            String json = String.format("{\"courseCode\":\"%s\", \"message\":\"%s\", \"creatorId\":\"%s\"}",
                    SessionManager.getCurrentCourseCode(),
                    message.replace("\"", "\\\"").replace("\n", "\\n"),
                    SessionManager.getCurrentUser().getId());

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/announcements"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json)).build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) handleCancel();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleCancel() {
        NavigationManager.switchScreen("announcements.fxml");
        NavigationManager.updateGlobalBreadcrumb("Announcements");
    }

    // You can keep or delete the handleAttachAssessment() method from the previous step,
    // since the @mention engine now does all the heavy lifting!
    @FXML
    private void handleAttachAssessment() {
        postBodyInput.appendText("@"); // Typing '@' manually triggers the menu!
    }
}