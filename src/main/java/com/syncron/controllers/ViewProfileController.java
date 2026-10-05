package com.syncron.controllers;

import com.syncron.utils.NavigationManager;
import com.syncron.utils.ServerConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class ViewProfileController {

    @FXML private Label nameLabel;
    @FXML private Label roleTag;
    @FXML private Label emailLabel;

    @FXML
    public void initialize() {
        // get the target id from memory, not the current user
        String targetProfileId = SessionManager.getViewProfileId();

        if (targetProfileId == null || targetProfileId.isEmpty()) {
            nameLabel.setText("User Not Found");
            return;
        }

        loadUserData(targetProfileId);
    }

    private void loadUserData(String userId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            Gson gson = new Gson();
            Type mapListType = new TypeToken<List<Map<String, Object>>>() {}.getType();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ServerConfig.getBaseUrl() + "/api/admin/all-users"))
                    .GET()
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                nameLabel.setText("Error loading profile");
                return;
            }

            List<Map<String, Object>> users = gson.fromJson(res.body(), mapListType);
            for (Map<String, Object> user : users) {
                if (!userId.equals(getText(user, "id"))) continue;

                String name = getText(user, "name");
                String role = getText(user, "role");
                String email = getText(user, "email");

                nameLabel.setText(name.isBlank() ? "Unknown User" : name);
                roleTag.setText(role.isBlank() ? "UNKNOWN" : role.toUpperCase());
                if ("STUDENT".equalsIgnoreCase(role)) {
                    roleTag.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 12px;");
                }

                emailLabel.setText("Email: " + (email.isBlank() ? "Not provided" : email));
                return;
            }

            nameLabel.setText("User Not Found");
        } catch (Exception e) {
            e.printStackTrace();
            nameLabel.setText("Error loading profile");
        }
    }

    @FXML
    private void goBack() {
        NavigationManager.switchScreen("evaluation_details.fxml");
    }

    private String getText(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}