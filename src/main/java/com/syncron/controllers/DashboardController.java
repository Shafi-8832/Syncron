package com.syncron.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import java.io.IOException;

public class DashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private BorderPane mainBorderPane; // This links to the fx:id you just added

    public void initialize() {
        welcomeLabel.setText("Welcome to Syncron, Shafi!");
    }

    @FXML
    private void handleShowCourses() {
        loadPage("courses"); // Calls the helper method below
    }

    private void loadPage(String pageName) {
        try {
            // This loads the new FXML file (courses.fxml)
            Parent root = FXMLLoader.load(getClass().getResource("/com/syncron/views/" + pageName + ".fxml"));

            // This replaces the CENTER of the dashboard with the new file
            mainBorderPane.setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This line is added to time travel.
}