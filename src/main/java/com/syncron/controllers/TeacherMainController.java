package com.syncron.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class TeacherMainController {

    @FXML private StackPane contentArea;
    @FXML private Label teacherNameLabel;

    @FXML
    public void initialize() {
        if (SessionManager.getCurrentUser() != null && teacherNameLabel != null) {
            teacherNameLabel.setText(SessionManager.getCurrentUser().getName());
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/home.fxml"));
            Parent home = loader.load();
            contentArea.getChildren().setAll(home);
        } catch (Exception e) {
            contentArea.getChildren().setAll(new Label("Unable to load dashboard content."));
        }
    }
}
