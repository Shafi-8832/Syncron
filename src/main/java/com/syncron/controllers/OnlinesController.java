package com.syncron.controllers;

import com.syncron.utils.DatabaseHandler;
import com.syncron.utils.NavigationManager;
import com.syncron.utils.TimeEngine;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OnlinesController {

    @FXML private Button createOnlineBtn;
    @FXML private VBox evaluationsContainer;

    @FXML
    public void initialize() {
        // Vault check: Hide Create button from students
        com.syncron.models.User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || !"TEACHER".equalsIgnoreCase(currentUser.getRole())) {
            createOnlineBtn.setVisible(false);
            createOnlineBtn.setManaged(false);
        }

        loadEvaluations();
    }

    private void loadEvaluations() {
        evaluationsContainer.getChildren().clear();
        String currentCourse = SessionManager.getCurrentCourseCode();
        boolean hasData = false;

        // 👉 Fetch ONLY 'ONLINE' evaluations
        String query = "SELECT id, title, start_date, start_time, deadline_date, deadline_time " +
                "FROM evaluations WHERE course_code = ? AND type = 'ONLINE' ORDER BY id DESC";

        try (java.sql.Connection conn = DatabaseHandler.connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, currentCourse);
            java.sql.ResultSet rs = pstmt.executeQuery();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            while (rs.next()) {
                hasData = true;

                VBox card = new VBox(10);
                card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E0D5C7; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(74,44,26,0.05), 5, 0, 0, 2); -fx-cursor: hand;");

                card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: #FFFCF8;"));
                card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-background-color: #FFFCF8;", "-fx-background-color: #FFFFFF;")));

                Label titleLabel = new Label(rs.getString("title"));
                titleLabel.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

                HBox statusBar = new HBox(15);
                statusBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label statusClockLabel = new Label("Calculating time...");

                String publishStr = rs.getString("start_date") + " " + rs.getString("start_time");
                String deadlineStr = rs.getString("deadline_date") + " " + rs.getString("deadline_time");

                try {
                    LocalDateTime startTime = LocalDateTime.parse(publishStr, formatter);
                    LocalDateTime endTime = LocalDateTime.parse(deadlineStr, formatter);
                    // Pass "ONLINE" so the TimeEngine knows to use the "Starts in:" logic
                    TimeEngine.startLiveCountdown(statusClockLabel, "ONLINE", startTime, endTime);
                } catch (Exception timeEx) {
                    statusClockLabel.setText("Ends: " + deadlineStr);
                    statusClockLabel.setStyle("-fx-text-fill: #7F8C8D;");
                }

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label arrow = new Label("View Details →");
                arrow.setStyle("-fx-text-fill: #D35400; -fx-font-weight: bold;");

                statusBar.getChildren().addAll(statusClockLabel, spacer, arrow);
                card.getChildren().addAll(titleLabel, statusBar);

                String evalId = rs.getString("id");
                card.setOnMouseClicked(e -> openEvaluationDetails(evalId));

                evaluationsContainer.getChildren().add(card);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!hasData) {
            Label emptyMsg = new Label("No online tests scheduled yet.");
            emptyMsg.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
            evaluationsContainer.getChildren().add(emptyMsg);
        }
    }

    private void openEvaluationDetails(String evaluationId) {
        SessionManager.setCurrentEvaluationId(evaluationId);
        NavigationManager.switchScreen("evaluation_details.fxml");
    }

    @FXML
    private void openCreateScreen() {
        // Pre-select the ONLINE toggle in the creation screen?
        // We can just open the same screen; the teacher can toggle it there.
        NavigationManager.switchScreen("sessional_evaluations.fxml");
    }
}