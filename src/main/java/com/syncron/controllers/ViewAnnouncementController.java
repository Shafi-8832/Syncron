package com.syncron.controllers;

import com.syncron.utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.control.Hyperlink;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ViewAnnouncementController {

    @FXML private Label authorLabel;
    @FXML private Label timeLabel;
    @FXML private VBox messageBodyContainer;
    @FXML private Button deleteBtn;
    @FXML private Button editBtn;


    private String postId;

    @FXML
    public void initialize() {
        postId = SessionManager.getCurrentAnnouncementId();
        if (postId == null) { handleBack(); return; }

        NavigationManager.updateGlobalBreadcrumb("Announcements / View Post");
        fetchPostData();
    }

    private void fetchPostData() {
        String courseCode = SessionManager.getCurrentCourseCode();
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/announcements/" + courseCode.replace(" ", "%20")))
                    .GET().build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType();
                List<Map<String, String>> posts = gson.fromJson(response.body(), listType);

                for (Map<String, String> post : posts) {
                    if (postId.equals(post.get("id"))) {
                        renderPost(post);
                        return;
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void renderPost(Map<String, String> post) {
        authorLabel.setText(post.get("author"));
        timeLabel.setText(post.get("timestamp"));

        if ("TEACHER".equals(SessionManager.getCurrentUser().getRole())) {
            deleteBtn.setVisible(true);
            deleteBtn.setManaged(true);
            editBtn.setVisible(true);
            editBtn.setManaged(true);
        }

        // THE DUAL-MODE SMART LINK PARSER
        TextFlow textFlow = new TextFlow();
        textFlow.setStyle("-fx-line-spacing: 6px;");
        String rawText = post.get("message");

        // This Regex magically catches BOTH Web URLs (Group 1) AND our Assessment Tags (Group 2 & 3)
        String dualRegex = "(https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])|\\[ASSESSMENT:(\\d+):([^\\]]+)\\]";
        Pattern pattern = Pattern.compile(dualRegex);
        Matcher matcher = pattern.matcher(rawText);

        int lastEnd = 0;
        while (matcher.find()) {
            // 1. Render the plain text BEFORE the link
            String plainText = rawText.substring(lastEnd, matcher.start());
            if (!plainText.isEmpty()) {
                Text t = new Text(plainText);
                t.setStyle("-fx-font-size: 16px; -fx-fill: #34495E; -fx-font-family: 'Inter', sans-serif;");
                textFlow.getChildren().add(t);
            }

            // 2. Check WHICH type of link we found
            if (matcher.group(1) != null) {
                // TYPE A: Standard Web URL
                String url = matcher.group(1);
                Hyperlink link = new Hyperlink(url);
                link.setStyle("-fx-font-size: 16px; -fx-text-fill: #2980B9; -fx-font-family: 'Inter', sans-serif; -fx-underline: true; -fx-padding: 0;");
                link.setOnAction(e -> {
                    try { Desktop.getDesktop().browse(new URI(url)); }
                    catch (Exception ex) { ex.printStackTrace(); }
                });
                textFlow.getChildren().add(link);

            }
             else if (matcher.group(2) != null) {
                // TYPE B: Internal Assessment Link!
                String evalId = matcher.group(2);
                String evalTitle = matcher.group(3);

                Hyperlink link = new Hyperlink(evalTitle); // Removed the emoji for a cleaner look

                // THE CSS FIX: a beautiful bold blue link now
                link.setStyle("-fx-font-size: 16px; -fx-text-fill: #2980B9; -fx-font-family: 'Inter', sans-serif; -fx-font-weight: bold; -fx-underline: true; -fx-padding: 0;");

                link.setOnAction(e -> {
                    SessionManager.setCurrentEvaluationId(evalId);
                    NavigationManager.switchScreen("evaluation_details.fxml");
                });
                textFlow.getChildren().add(link);
            }
            lastEnd = matcher.end();
        }

        // 3. Render any remaining text AFTER the last link
        String tail = rawText.substring(lastEnd);
        if (!tail.isEmpty()) {
            Text t = new Text(tail);
            t.setStyle("-fx-font-size: 16px; -fx-fill: #34495E; -fx-font-family: 'Inter', sans-serif;");
            textFlow.getChildren().add(t);
        }

        messageBodyContainer.getChildren().clear();
        messageBodyContainer.getChildren().add(textFlow);
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this announcement?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
        if (confirm.getResult() == ButtonType.YES) {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/announcements/" + postId))
                        .DELETE().build();
                client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                handleBack();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void handleBack() {
        NavigationManager.switchScreen("announcements.fxml");
    }

    @FXML
    private void handleEdit() {
        SessionManager.setEditAnnouncementId(postId);
        NavigationManager.switchScreen("create_announcement.fxml");
    }
}