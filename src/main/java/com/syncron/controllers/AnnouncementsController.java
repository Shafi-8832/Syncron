package com.syncron.controllers;

import com.syncron.utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Map;

public class AnnouncementsController {

    @FXML private VBox feedContainer;
    @FXML private Button createPostBtn;

    private boolean isTeacher = false;

    @FXML
    public void initialize() {
        String currentCourse = SessionManager.getCurrentCourseCode();
        if (currentCourse == null || currentCourse.isEmpty()) return;

        if ("TEACHER".equals(SessionManager.getCurrentUser().getRole())) {
            isTeacher = true;
            createPostBtn.setVisible(true);
            createPostBtn.setManaged(true);
        }
        loadAnnouncements(currentCourse);
    }

    private void loadAnnouncements(String courseCode) {
        feedContainer.getChildren().clear();
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/announcements/" + courseCode.replace(" ", "%20")))
                    .GET().build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType();
                List<Map<String, String>> posts = gson.fromJson(response.body(), listType);

                if (posts.isEmpty()) {
                    Label empty = new Label("No announcements have been posted yet.");
                    empty.setStyle("-fx-text-fill: #7F8C8D; -fx-font-style: italic;");
                    feedContainer.getChildren().add(empty);
                    return;
                }
                for (Map<String, String> post : posts) feedContainer.getChildren().add(createPostCard(post));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private VBox createPostCard(Map<String, String> post) {
        VBox card = new VBox(10);

        //THE PREMIUM LIGHT BLUE GLOWING UPGRADE + HOVER EFFECT
        String baseStyle = "-fx-background-color: #F4FAFE; -fx-border-color: #AED6F1; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(52, 152, 219, 0.2), 10, 0, 0, 0);";
        String hoverStyle = "-fx-background-color: #EBF5FB; -fx-border-color: #3498DB; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(52, 152, 219, 0.4), 15, 0, 0, 0);";

        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        //ROUTE TO THE FACEBOOK-STYLE POST WHEN CLICKED
        card.setOnMouseClicked(e -> {
            SessionManager.setCurrentAnnouncementId(post.get("id"));
            NavigationManager.switchScreen("view_announcement.fxml");
        });

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label author = new Label(post.get("author"));
        author.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50; -fx-font-size: 14px;");
        Label dot = new Label("•");
        dot.setStyle("-fx-text-fill: #BDC3C7;");
        Label time = new Label(post.get("timestamp"));
        time.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(author, dot, time, spacer);

        if (isTeacher) {
            Button delBtn = new Button("Delete");
            delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E74C3C; -fx-cursor: hand; -fx-font-size: 11px; -fx-underline: true;");
            delBtn.setOnAction(e -> {
                e.consume(); // PREVENTS the click from opening the post!
                deletePost(post.get("id"));
            });
            header.getChildren().add(delBtn);
        }

// Clean the preview so smart-tags look beautiful
        String cleanPreview = post.get("message").replaceAll("\\[ASSESSMENT:\\d+:([^\\]]+)\\]", "📌 $1");
        Label message = new Label(cleanPreview);
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #34495E; -fx-font-size: 14px; -fx-padding: 5 0 0 0;");

        if (post.get("message").startsWith("🔔 New")) {
            card.setStyle(card.getStyle() + " -fx-border-left-color: #3498DB; -fx-border-left-width: 4;");
        }

        card.getChildren().addAll(header, message);
        return card;
    }

    @FXML
    private void handleCreatePost() {
        // Goodbye Windows 98 Dialog, Hello SPA Routing!
        NavigationManager.switchScreen("create_announcement.fxml");
    }

    private void sendPostToCloud(String message) {
        try {
            String json = String.format("{\"courseCode\":\"%s\", \"message\":\"%s\", \"creatorId\":\"%s\"}",
                    SessionManager.getCurrentCourseCode(), message, SessionManager.getCurrentUser().getId());

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/announcements"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();
            client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            loadAnnouncements(SessionManager.getCurrentCourseCode());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void deletePost(String id) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this announcement?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
        if (confirm.getResult() == ButtonType.YES) {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/announcements/" + id))
                        .DELETE().build();
                client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                loadAnnouncements(SessionManager.getCurrentCourseCode());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}