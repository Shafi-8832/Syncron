package com.syncron.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML
    private Label welcomeLabel;

    public void initialize() {
        // This runs automatically when the app starts
        welcomeLabel.setText("Welcome, Ahnaf Ahmed Shafi!");
    }
}