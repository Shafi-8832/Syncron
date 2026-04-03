package com.syncron.controllers;

import com.syncron.utils.DatabaseHandler;
import com.syncron.utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

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
        String query = "SELECT name, role, email FROM users WHERE id = ?";

        try (java.sql.Connection conn = DatabaseHandler.connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, userId);
            java.sql.ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                nameLabel.setText(rs.getString("name"));

                String role = rs.getString("role");
                roleTag.setText(role.toUpperCase());

                // style the tag differently if they are a student
                if ("STUDENT".equalsIgnoreCase(role)) {
                    roleTag.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 12px;");
                }

                String email = rs.getString("email");
                emailLabel.setText("Email: " + (email != null ? email : "Not provided"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            nameLabel.setText("Error loading profile");
        }
    }

    @FXML
    private void goBack() {
        // route back to the assessment details page safely
        NavigationManager.switchScreen("evaluation_details.fxml");
    }
}