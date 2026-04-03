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

        SessionManager.setLastSidebarTab("ONLINE");

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

        // 1. fetch the data into a temporary list so we can sort it
        class EvalCard {
            String id, title, type, startStr, endStr;
            java.time.LocalDateTime startTime, endTime;
            boolean isPast = false;
        }
        java.util.List<EvalCard> cardList = new java.util.ArrayList<>();

        // fetch ONLY 'ONLINE' evaluations
        String query = "SELECT id, title, type, start_date, start_time, deadline_date, deadline_time " +
                "FROM evaluations WHERE course_code = ? AND type = 'ONLINE'";

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        try (java.sql.Connection conn = DatabaseHandler.connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, currentCourse);
            java.sql.ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                EvalCard c = new EvalCard();
                c.id = rs.getString("id");
                c.title = rs.getString("title");
                c.type = rs.getString("type");
                c.startStr = rs.getString("start_date") + " " + rs.getString("start_time");
                c.endStr = rs.getString("deadline_date") + " " + rs.getString("deadline_time");

                try {
                    c.startTime = java.time.LocalDateTime.parse(c.startStr, formatter);
                    c.endTime = java.time.LocalDateTime.parse(c.endStr, formatter);
                    if (java.time.LocalDateTime.now().isAfter(c.endTime)) {
                        c.isPast = true; // flag it as expired
                    }
                } catch (Exception e) {}

                cardList.add(c);
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (cardList.isEmpty()) {
            Label emptyMsg = new Label("No online lab tests scheduled yet.");
            emptyMsg.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
            evaluationsContainer.getChildren().add(emptyMsg);
            return;
        }

        // 2. sorting engine: active first, then by id descending
        cardList.sort((a, b) -> {
            if (a.isPast != b.isPast) return Boolean.compare(a.isPast, b.isPast);
            return Integer.compare(Integer.parseInt(b.id), Integer.parseInt(a.id));
        });

        // 3. rendering engine
        int index = 1;
        for (EvalCard c : cardList) {

            HBox cardRow = new HBox(15);
            cardRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // the numbered circle
            javafx.scene.layout.StackPane circlePane = new javafx.scene.layout.StackPane();
            javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(16);
            circle.setFill(javafx.scene.paint.Color.web(c.isPast ? "#BDC3C7" : "#F39C12"));
            Label numLabel = new Label(String.valueOf(index++));
            numLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
            circlePane.getChildren().addAll(circle, numLabel);

            // the card itself
            VBox card = new VBox(10);
            HBox.setHgrow(card, Priority.ALWAYS);

            // premium css: glowing if active, dimmed if past
            if (c.isPast) {
                card.setStyle("-fx-background-color: #F8F9FA; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 20; -fx-opacity: 0.7;");
            } else {
                card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #F39C12; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 19; -fx-effect: dropshadow(three-pass-box, rgba(243, 156, 18, 0.25), 15, 0, 0, 0); -fx-cursor: hand;");
                card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: #FFFDF8;"));
                card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-background-color: #FFFDF8;", "-fx-background-color: #FFFFFF;")));
            }

            Label titleLabel = new Label(c.title);
            titleLabel.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

            HBox statusBar = new HBox(15);
            statusBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label statusClockLabel = new Label("Calculating time...");

            if (c.startTime != null && c.endTime != null) {
                TimeEngine.startLiveCountdown(statusClockLabel, "ONLINE", c.startTime, c.endTime);
            } else {
                statusClockLabel.setText("Ends: " + c.endStr);
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label arrow = new Label("View Details →");                                               //
            arrow.setStyle(c.isPast ? "-fx-text-fill: #95A5A6; -fx-font-weight: bold;" : "-fx-text-fill: #F39C12; -fx-font-weight: bold;");

            statusBar.getChildren().addAll(statusClockLabel, spacer, arrow);
            card.getChildren().addAll(titleLabel, statusBar);

            // click routing
            card.setOnMouseClicked(e -> {
                SessionManager.setCurrentEvaluationId(c.id);
                NavigationManager.switchScreen("evaluation_details.fxml");
            });

            cardRow.getChildren().addAll(circlePane, card);
            evaluationsContainer.getChildren().add(cardRow);
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