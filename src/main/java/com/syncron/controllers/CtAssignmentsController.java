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

public class CtAssignmentsController {

    @FXML private Button createBtn;
    @FXML private VBox evaluationsContainer;

    @FXML
    public void initialize() {
        com.syncron.models.User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || !"TEACHER".equalsIgnoreCase(currentUser.getRole())) {
            createBtn.setVisible(false);
            createBtn.setManaged(false);
        }
        loadEvaluations();
    }

    private void loadEvaluations() {
        evaluationsContainer.getChildren().clear();
        String currentCourse = SessionManager.getCurrentCourseCode();

        // 1. Fetch the data into a temporary list so we can sort it
        class EvalCard {
            String id, title, type, startStr, endStr;
            LocalDateTime startTime, endTime;
            boolean isPast = false;
        }
        java.util.List<EvalCard> cardList = new java.util.ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        //DOUBLE CLOUD FETCH (CTs + Assignments)
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType();

            String[] typesToFetch = {"CT", "ASSIGNMENT"};
            for (String typeToFetch : typesToFetch) {
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/evaluations/course/" + currentCourse.replace(" ", "%20") + "/" + typeToFetch))
                        .GET().build();

                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    java.util.List<java.util.Map<String, String>> evals = gson.fromJson(response.body(), listType);
                    for (java.util.Map<String, String> rs : evals) {
                        EvalCard c = new EvalCard();
                        c.id = rs.get("id");
                        c.title = rs.get("title");
                        c.type = rs.get("type");
                        c.startStr = rs.get("startDate") + " " + rs.get("startTime");
                        c.endStr = rs.get("deadlineDate") + " " + rs.get("deadlineTime");

                        try {
                            c.startTime = LocalDateTime.parse(c.startStr, formatter);
                            c.endTime = LocalDateTime.parse(c.endStr, formatter);
                            if (LocalDateTime.now().isAfter(c.endTime)) c.isPast = true;
                        } catch (Exception ignored) {}

                        cardList.add(c);
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (cardList.isEmpty()) {
            Label emptyMsg = new Label("No assessments scheduled yet.");
            emptyMsg.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
            evaluationsContainer.getChildren().add(emptyMsg);
            return;
        }

        // 2. SORTING ENGINE: Active first, then by ID descending
        cardList.sort((a, b) -> {
            if (a.isPast != b.isPast) return Boolean.compare(a.isPast, b.isPast);
            return Integer.compare(Integer.parseInt(b.id), Integer.parseInt(a.id));
        });

        // 3. RENDERING ENGINE: Build the og op UI
        int index = 1;
        for (EvalCard c : cardList) {

            HBox cardRow = new HBox(15);
            cardRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // THE NUMBERED CIRCLE
            javafx.scene.layout.StackPane circlePane = new javafx.scene.layout.StackPane();
            javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(16);
            circle.setFill(javafx.scene.paint.Color.web(c.isPast ? "#BDC3C7" : "#D35400")); // Gray if dead, Orange if alive
            Label numLabel = new Label(String.valueOf(index++));
            numLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
            circlePane.getChildren().addAll(circle, numLabel);

            // THE CARD ITSELF
            VBox card = new VBox(10);
            HBox.setHgrow(card, Priority.ALWAYS); // Make card stretch to fill space

            // Premium CSS: Glowing if active, Dimmed if past
            if (c.isPast) {
                card.setStyle("-fx-background-color: #F8F9FA; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 20; -fx-opacity: 0.7;");
            } else {
                card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #F39C12; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 19; -fx-effect: dropshadow(three-pass-box, rgba(243, 156, 18, 0.25), 15, 0, 0, 0); -fx-cursor: hand;");
                card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: #FFFDF8;"));
                card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-background-color: #FFFDF8;", "-fx-background-color: #FFFFFF;")));
            }

            HBox titleBox = new HBox(10);
            titleBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label titleLabel = new Label(c.title);
            titleLabel.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

            Label typeTag = new Label(c.type);
            typeTag.setStyle(c.type.equals("CT") ? "-fx-background-color: #FDEBD0; -fx-text-fill: #D35400; -fx-padding: 3 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;"
                    : "-fx-background-color: #D5F5E3; -fx-text-fill: #27AE60; -fx-padding: 3 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            titleBox.getChildren().addAll(titleLabel, typeTag);

            HBox statusBar = new HBox(15);
            statusBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label statusClockLabel = new Label("Calculating time...");

            if (c.startTime != null && c.endTime != null) {
                String engineType = c.type.equals("CT") ? "ONLINE" : "OFFLINE";
                TimeEngine.startLiveCountdown(statusClockLabel, engineType, c.startTime, c.endTime);
            } else {
                statusClockLabel.setText("Due: " + c.endStr);
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label arrow = new Label("View Details →");
            arrow.setStyle(c.isPast ? "-fx-text-fill: #95A5A6; -fx-font-weight: bold;" : "-fx-text-fill: #D35400; -fx-font-weight: bold;");

            statusBar.getChildren().addAll(statusClockLabel, spacer, arrow);
            card.getChildren().addAll(titleBox, statusBar);

            // Click routing
            card.setOnMouseClicked(e -> {
                SessionManager.setCurrentEvaluationId(c.id);
                NavigationManager.switchScreen("evaluation_details.fxml");
            });

            // Add circle and card to the row
            cardRow.getChildren().addAll(circlePane, card);
            evaluationsContainer.getChildren().add(cardRow);
        }
    }

    @FXML
    private void openCreateScreen() {
        NavigationManager.switchScreen("theory_evaluations.fxml");
    }

}