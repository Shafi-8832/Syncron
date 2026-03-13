package com.syncron.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML
    private Label welcomeLabel;

    public void initialize() {
        welcomeLabel.setText("Welcome to Syncron, Shafi!");
    }
}