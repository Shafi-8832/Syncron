package com.syncron.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class StudentMainController {

    @FXML
    private BorderPane mainRootPane;

    @FXML
    private StackPane contentArea;

    public void initialize() {
        // Load the default dashboard view on startup
        loadPage("dashboard");
    }

    @FXML
    private void handleShowDashboard() {
        loadPage("dashboard");
    }

    @FXML
    private void handleShowCourses() {
        loadPage("courses");
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) mainRootPane.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 500));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadPage(String pageName) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource("/com/syncron/views/" + pageName + ".fxml"));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
