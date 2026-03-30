package com.syncron.controllers;

import com.syncron.utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class OfflinesController {

    @FXML private Button createOfflineBtn;
    @FXML private VBox evaluationsContainer;

    @FXML
    public void initialize() {
        // 1. Grab the current user and check their role safely
        com.syncron.models.User currentUser = SessionManager.getCurrentUser();

        if (currentUser == null || !"TEACHER".equalsIgnoreCase(currentUser.getRole())) {
            // If they aren't a teacher (or aren't logged in properly), hide the button!
            createOfflineBtn.setVisible(false);
            createOfflineBtn.setManaged(false);
        }

        // 2. Load the existing offlines
        loadEvaluations();
    }

    private void loadEvaluations() {
        evaluationsContainer.getChildren().clear();
        // Placeholder for now, until we build the DB fetcher for the list!
        Label emptyMsg = new Label("No offlines published yet.");
        emptyMsg.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
        evaluationsContainer.getChildren().add(emptyMsg);
    }

    @FXML
    private void openCreateScreen() {
        // Teleport the teacher to the Creation Canvas!
        NavigationManager.switchScreen("sessional_evaluations.fxml");
    }
}